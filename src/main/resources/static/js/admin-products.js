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
    const action = button.dataset.action;
    const messages = { publish: 'Publish this draft?', archive: 'Archive this active product?', delete: 'Permanently delete this draft?' };
    if (!window.confirm(messages[action])) return;
    button.disabled = true;
    const method = action === 'delete' ? 'DELETE' : 'PATCH';
    try {
      const response = await fetch(`/api/admin/products/${row.dataset.productId}${action === 'delete' ? '' : `/${action}`}`, {
        method, headers: csrfHeaders()
      });
      if (!response.ok) throw new Error(await responseMessage(response));
      const notices = { publish: 'Product published.', archive: 'Product archived.', delete: 'Draft deleted.' };
      window.location.assign(`/admin/products?notice=${encodeURIComponent(notices[action])}`);
    } catch (error) {
      showListFeedback(error.message, true);
      button.disabled = false;
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
if (form) {
  document.querySelector('[data-add-variant]').addEventListener('click', () => addRow('variant-row-template', '[data-variant-list]'));
  document.querySelector('[data-add-image]').addEventListener('click', () => addRow('image-row-template', '[data-image-list]'));
  form.addEventListener('click', (event) => {
    const remove = event.target.closest('[data-remove-row]');
    if (remove) remove.closest('.repeat-row').remove();
  });
  form.addEventListener('submit', submitProduct);
}

function addRow(templateId, listSelector) {
  const fragment = document.getElementById(templateId).content.cloneNode(true);
  document.querySelector(listSelector).append(fragment);
}

async function submitProduct(event) {
  event.preventDefault();
  clearErrors();
  if (!form.reportValidity()) return;
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

function productPayload() {
  const value = (name) => form.elements.namedItem(name).value.trim();
  return {
    name: value('name'), description: value('description'), brand: value('brand'), collectionName: value('collectionName'),
    category: value('category'), basePrice: Number(value('basePrice')), discountPercentage: Number(value('discountPercentage')),
    variants: [...form.querySelectorAll('.variant-row')].map((row) => ({
      sku: field(row, 'sku'), size: field(row, 'size'), colorName: field(row, 'colorName'), colorHex: field(row, 'colorHex') || null,
      stockQuantity: Number(field(row, 'stockQuantity'))
    })),
    images: [...form.querySelectorAll('.image-row')].map((row) => ({
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
