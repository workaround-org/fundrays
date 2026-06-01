(function () {
  const form = document.getElementById('donate-form');
  if (!form) return;

  const slug = form.dataset.slug;
  const amountInput = document.getElementById('amount-euros');
  const errorBox = document.getElementById('form-error');
  const submitBtn = document.getElementById('submit-btn');
  const MIN_EUROS = 5;

  function clearAmountButtons() {
    document.querySelectorAll('.amount-btn').forEach(function (b) { b.classList.remove('selected'); });
  }

  document.querySelectorAll('.amount-btn').forEach(function (btn) {
    btn.addEventListener('click', function () {
      amountInput.value = btn.dataset.euros;
      clearAmountButtons();
      btn.classList.add('selected');
    });
  });
  amountInput.addEventListener('input', clearAmountButtons);

  document.querySelectorAll('.pay-option input').forEach(function (radio) {
    radio.addEventListener('change', function () {
      document.querySelectorAll('.pay-option').forEach(function (o) { o.classList.remove('selected'); });
      if (radio.checked) radio.closest('.pay-option').classList.add('selected');
    });
  });

  function showError(msg) {
    errorBox.textContent = msg;
    errorBox.style.display = 'block';
  }

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    errorBox.style.display = 'none';

    const euros = parseFloat(amountInput.value);
    if (isNaN(euros) || euros < MIN_EUROS) {
      showError('Bitte gib einen Betrag von mindestens 5,00 € ein.');
      return;
    }
    const method = form.querySelector('input[name="paymentMethod"]:checked');
    if (!method) {
      showError('Bitte wähle eine Zahlungsart.');
      return;
    }

    const payload = {
      amount: Math.round(euros * 100),
      paymentMethod: method.value,
      donorName: form.donorName.value || null,
      donorEmail: form.donorEmail.value || null,
      message: form.message.value || null
    };

    submitBtn.disabled = true;
    fetch('/api/public/campaigns/' + encodeURIComponent(slug) + '/donate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).then(function (res) {
      if (!res.ok) throw new Error('status ' + res.status);
      return res.json();
    }).then(function (data) {
      window.location = data.paymentUrl;
    }).catch(function () {
      submitBtn.disabled = false;
      showError('Die Spende konnte nicht verarbeitet werden. Bitte versuche es erneut.');
    });
  });

  function refreshProgress() {
    fetch('/api/public/campaigns/' + encodeURIComponent(slug) + '/progress')
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (p) {
        if (!p) return;
        const raised = (p.raisedAmount / 100).toLocaleString('de-DE', {
          minimumFractionDigits: 2, maximumFractionDigits: 2
        }) + ' €';
        document.getElementById('raised').textContent = raised;
        document.getElementById('prog-bar').style.width = p.percentage + '%';
        document.getElementById('percent').textContent = p.percentage + '%';
        document.getElementById('count').textContent = p.donationCount;
      })
      .catch(function () {});
  }
  setInterval(refreshProgress, 5000);
})();
