const autoSubmitFields = document.querySelectorAll('[data-auto-submit] select');
autoSubmitFields.forEach((select) => select.addEventListener('change', () => select.form.requestSubmit()));

function csrfHeaders() {
  const token = document.querySelector('meta[name="_csrf"]')?.content;
  const header = document.querySelector('meta[name="_csrf_header"]')?.content;
  return token && header ? { [header]: token } : {};
}

const list = document.querySelector('[data-admin-list]');
if (list) {
  list.addEventListener('click', async (event) => {
    const button = event.target.closest('[data-action]');
    if (!button) return;
    const row = button.closest('[data-product-id]');
    if (row.dataset.busy === 'true') return;
    const action = button.dataset.action;
    const status = row.dataset.productStatus;
    const confirmation = action === 'delete'
      ? (status === 'ACTIVE'
          ? 'This product is currently published. Delete it permanently and remove it from the catalog?'
          : 'Delete this product permanently?')
      : action === 'publish'
        ? (status === 'ARCHIVED' ? 'Republish this archived product?' : 'Publish this draft?')
        : 'Archive this active product?';
    if (!window.confirm(confirmation)) return;
    row.dataset.busy = 'true';
    row.querySelectorAll('[data-action]').forEach((actionButton) => { actionButton.disabled = true; });
    const method = action === 'delete' ? 'DELETE' : 'PATCH';
    try {
      const response = await fetch(`/api/admin/products/${row.dataset.productId}${action === 'delete' ? '' : `/${action}`}`, {
        method, headers: csrfHeaders()
      });
      if (!response.ok) throw new Error(await responseMessage(response));
      const notice = action === 'publish' && status === 'ARCHIVED'
        ? 'Product republished.'
        : { publish: 'Product published.', archive: 'Product archived.', delete: 'Product deleted.' }[action];
      window.location.assign(`/admin/products?notice=${encodeURIComponent(notice)}`);
    } catch (error) {
      showListFeedback(error.message, true);
      row.dataset.busy = 'false';
      row.querySelectorAll('[data-action]').forEach((actionButton) => { actionButton.disabled = false; });
    }
  });
}

function showListFeedback(message, isError) {
  const feedback = document.querySelector('.feedback');
  feedback.textContent = message;
  feedback.classList.toggle('feedback--error', isError);
  feedback.classList.remove('is-hidden');
  feedback.focus?.();
}

const form = document.querySelector('[data-product-form]');
let activeUploads = 0;
const uploadsInFlight = new Set();
if (form) {
  document.querySelector('[data-add-variant]').addEventListener('click', () => addRow('variant-row-template', '[data-variant-list]'));
  document.querySelector('[data-add-image]').addEventListener('click', () => addRow('image-row-template', '[data-image-list]'));
  form.addEventListener('click', (event) => {
    const remove = event.target.closest('[data-remove-row]');
    if (remove) remove.closest('.repeat-row').remove();
  });
  form.addEventListener('submit', submitProduct);
  initializeImageUploads();
  form.querySelectorAll('.image-row').forEach(bindImagePreview);
}

function addRow(templateId, listSelector) {
  const fragment = document.getElementById(templateId).content.cloneNode(true);
  const row = fragment.querySelector('.repeat-row');
  document.querySelector(listSelector).append(fragment);
  if (row?.classList.contains('image-row')) bindImagePreview(row);
}

async function submitProduct(event) {
  event.preventDefault();
  clearErrors();
  if (!form.reportValidity()) return;
  if (activeUploads > 0) {
    showFormError('Wait for all image uploads to finish before saving.');
    return;
  }
  const submit = form.querySelector('[data-submit]');
  submit.disabled = true;
  submit.textContent = 'Saving…';
  const editing = form.dataset.mode === 'edit';
  try {
    const response = await fetch(editing ? `/api/admin/products/${form.dataset.productId}` : '/api/admin/products', {
      method: editing ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json', ...csrfHeaders() }, body: JSON.stringify(productPayload())
    });
    if (!response.ok) {
      const problem = await responseProblem(response);
      applyFieldErrors(problem.fieldErrors);
      throw new Error(problem.message);
    }
    window.location.assign(`/admin/products?notice=${encodeURIComponent(editing ? 'Product updated.' : 'Draft created.')}`);
  } catch (error) {
    const feedback = document.querySelector('[data-form-error]');
    feedback.textContent = error.message;
    feedback.classList.remove('is-hidden');
    submit.disabled = false;
    submit.textContent = editing ? 'Save changes' : 'Create draft';
  }
}

function initializeImageUploads() {
  const zone = document.querySelector('[data-image-drop-zone]');
  const input = document.querySelector('[data-image-files]');
  document.querySelector('[data-choose-images]').addEventListener('click', (event) => { event.stopPropagation(); input.click(); });
  zone.addEventListener('click', (event) => { if (!event.target.closest('button')) input.click(); });
  zone.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); input.click(); }
  });
  ['dragenter', 'dragover'].forEach((name) => zone.addEventListener(name, (event) => {
    event.preventDefault(); zone.classList.add('is-dragging');
  }));
  ['dragleave', 'drop'].forEach((name) => zone.addEventListener(name, (event) => {
    event.preventDefault(); zone.classList.remove('is-dragging');
  }));
  zone.addEventListener('drop', (event) => uploadFiles(event.dataTransfer.files));
  input.addEventListener('change', () => { uploadFiles(input.files); input.value = ''; });
}

function uploadFiles(files) { [...files].forEach(uploadFile); }

async function uploadFile(file) {
  const fingerprint = `${file.name}:${file.size}:${file.lastModified}`;
  if (uploadsInFlight.has(fingerprint)) return;
  uploadsInFlight.add(fingerprint); activeUploads += 1;
  addRow('image-row-template', '[data-image-list]');
  const row = document.querySelector('[data-image-list] .image-row:last-child');
  row.classList.add('is-uploading');
  row.querySelector('[data-field="altText"]').value = file.name.replace(/\.[^.]+$/, '');
  updateUploadStatus();
  try {
    const body = new FormData(); body.append('file', file);
    const response = await fetch('/api/admin/product-images', { method: 'POST', headers: csrfHeaders(), body });
    if (!response.ok) throw new Error((await responseProblem(response)).message);
    const uploaded = await response.json();
    row.querySelector('[data-field="url"]').value = uploaded.url;
    refreshPreview(row);
  } catch (error) {
    row.querySelector('[data-upload-error]').textContent = error.message;
    row.classList.add('has-upload-error');
  } finally {
    activeUploads -= 1; uploadsInFlight.delete(fingerprint); row.classList.remove('is-uploading'); updateUploadStatus();
  }
}

function updateUploadStatus() {
  document.querySelector('[data-upload-status]').textContent = activeUploads ? `${activeUploads} image upload(s) in progress…` : '';
}

function bindImagePreview(row) {
  const input = row.querySelector('[data-field="url"]');
  input.addEventListener('change', () => refreshPreview(row));
  const image = row.querySelector('[data-preview]');
  image.addEventListener('error', () => row.classList.add('preview-failed'));
  image.addEventListener('load', () => row.classList.remove('preview-failed'));
  refreshPreview(row);
}

function refreshPreview(row) {
  row.querySelector('[data-preview]').src = row.querySelector('[data-field="url"]').value.trim();
}

function showFormError(message) {
  const feedback = document.querySelector('[data-form-error]');
  feedback.textContent = message; feedback.classList.remove('is-hidden'); feedback.focus?.();
}

function productPayload() {
  const value = (name) => form.elements.namedItem(name).value.trim();
  return {
    name: value('name'), description: value('description'), brand: value('brand'), collectionName: value('collectionName'),
    category: value('category'), basePrice: Number(value('basePrice')), discountPercentage: Number(value('discountPercentage')),
    variants: [...form.querySelectorAll('.variant-row')].map((row) => ({
      id: row.dataset.childId || null,
      sku: field(row, 'sku'), size: field(row, 'size'), colorName: field(row, 'colorName'), colorHex: field(row, 'colorHex') || null,
      stockQuantity: Number(field(row, 'stockQuantity'))
    })),
    images: [...form.querySelectorAll('.image-row')].map((row) => ({
      id: row.dataset.childId || null,
      url: field(row, 'url'), altText: field(row, 'altText') || null, displayOrder: Number(field(row, 'displayOrder')),
      primaryImage: row.querySelector('[data-field="primaryImage"]').checked
    }))
  };
}

function field(row, name) { return row.querySelector(`[data-field="${name}"]`).value.trim(); }
function clearErrors() {
  document.querySelector('[data-form-error]').classList.add('is-hidden');
  document.querySelectorAll('.field-error').forEach((node) => node.textContent = '');
}
function applyFieldErrors(errors = {}) {
  Object.entries(errors).forEach(([name, messages]) => {
    const root = name.split(/[.\[]/)[0];
    const target = document.querySelector(`[data-error-for="${CSS.escape(root)}"]`);
    if (target) target.textContent = messages.join(' ');
  });
}
async function responseProblem(response) {
  const type = response.headers.get('content-type') || '';
  if (!type.includes('application/json')) return { message: `Request failed (${response.status}).`, fieldErrors: {} };
  const body = await response.json();
  return { message: body.message || `Request failed (${response.status}).`, fieldErrors: body.fieldErrors || {} };
}
async function responseMessage(response) { return (await responseProblem(response)).message; }
