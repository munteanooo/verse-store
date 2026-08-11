document.querySelectorAll('[data-auto-submit] select').forEach((select) => {
  select.addEventListener('change', () => select.form.requestSubmit());
});

document.querySelectorAll('[data-image-fallback]').forEach((image) => {
  image.addEventListener('error', () => image.classList.add('is-broken'), { once: true });
});
