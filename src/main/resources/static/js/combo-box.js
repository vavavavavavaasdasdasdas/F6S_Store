(() => {
  if (window.__comboBoxInitialized) return;
  window.__comboBoxInitialized = true;

  let instanceCounter = 0;

  function foldVi(str) {
    if (!str) return '';
    return String(str)
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd')
      .replace(/Đ/g, 'd')
      .toLowerCase()
      .replace(/\s+/g, ' ')
      .trim();
  }

  function cleanSpaces(str) {
    return String(str || '').replace(/\s+/g, ' ').trim();
  }

  function enhanceSelect(select) {
    if (!select || select.dataset.comboEnhanced === 'true' || select.multiple) return;
    select.dataset.comboEnhanced = 'true';
    const uid = ++instanceCounter;
    const listboxId = (select.id ? select.id : 'combo-' + uid) + '-listbox';
    const isCreatable = select.dataset.creatable === 'true';
    const createInputSelector = select.dataset.createInput || '';
    const createValueMode = select.dataset.createValueMode || 'id';
    const isCheckboxSync = select.dataset.sizeCheckboxSync === 'true';
    const maxLen = Number(select.dataset.maxlength || 100);

    select.classList.add('combo-native-select');
    select.tabIndex = -1;
    select.setAttribute('aria-hidden', 'true');

    const wrapper = document.createElement('div');
    wrapper.className = 'combo-box';
    wrapper.dataset.comboWrapper = 'true';
    if (select.id) wrapper.dataset.comboFor = select.id;

    const control = document.createElement('div');
    control.className = 'combo-control';

    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'form-control combo-input';
    if (select.classList.contains('form-select-sm')) {
      input.classList.add('form-control-sm');
    }
    input.dataset.comboInput = 'true';
    input.setAttribute('role', 'combobox');
    input.setAttribute('aria-autocomplete', 'list');
    input.setAttribute('aria-expanded', 'false');
    input.setAttribute('aria-controls', listboxId);
    input.autocomplete = 'off';
    input.spellcheck = false;
    if (maxLen > 0 && isCreatable) input.maxLength = maxLen;

    const clearBtn = document.createElement('button');
    clearBtn.type = 'button';
    clearBtn.className = 'combo-clear';
    clearBtn.dataset.comboClear = 'true';
    clearBtn.setAttribute('aria-label', 'Xóa lựa chọn');
    clearBtn.tabIndex = -1;
    clearBtn.textContent = '×';
    clearBtn.hidden = true;

    const toggleBtn = document.createElement('button');
    toggleBtn.type = 'button';
    toggleBtn.className = 'combo-toggle';
    toggleBtn.dataset.comboToggle = 'true';
    toggleBtn.setAttribute('aria-label', 'Mở danh sách');
    toggleBtn.tabIndex = -1;
    toggleBtn.textContent = '▾';

    const listbox = document.createElement('ul');
    listbox.className = 'combo-listbox';
    listbox.id = listboxId;
    listbox.setAttribute('role', 'listbox');
    listbox.hidden = true;

    const errorEl = document.createElement('p');
    errorEl.className = 'combo-error';
    errorEl.dataset.comboError = 'true';
    errorEl.setAttribute('aria-live', 'polite');
    errorEl.hidden = true;

    control.append(input, clearBtn, toggleBtn);
    wrapper.append(control, listbox, errorEl);
    select.after(wrapper);

    select._comboInput = input;
    select._comboWrapper = wrapper;

    if (select.id) {
      const label = document.querySelector(`label[for="${CSS.escape(select.id)}"]`);
      if (label) {
        if (!label.id) label.id = select.id + '-combo-label';
        input.setAttribute('aria-labelledby', label.id);
        label.addEventListener('click', (e) => {
          if (e.target.closest('.combo-box')) return;
          e.preventDefault();
          if (!input.disabled) input.focus();
        });
      }
    }
    if (!input.hasAttribute('aria-labelledby') && !input.hasAttribute('aria-label')) {
      const parentLabel = select.closest('label');
      if (parentLabel) {
        const text = cleanSpaces(parentLabel.childNodes[0]?.textContent || parentLabel.textContent);
        if (text) input.setAttribute('aria-label', text);
      } else {
        input.setAttribute('aria-label', select.name || 'Chọn giá trị');
      }
    }

    let isFiltering = false;
    let activeIndex = -1;
    let currentItems = [];
    let internalMutation = false;

    function getCompanionInput() {
      if (!createInputSelector) return null;
      return (select.form || document).querySelector(createInputSelector);
    }

    function getDefaultOption() {
      const opts = [...select.options].filter(o => !o.dataset.createdOption);
      const emptyOpt = opts.find(o => o.value === '');
      if (emptyOpt) return emptyOpt;
      if (!select.required && opts[0] && (opts[0].value === 'all' || opts[0].value === 'ALL')) {
        return opts[0];
      }
      return null;
    }

    function getDefaultEmptyValue() {
      const def = getDefaultOption();
      return def ? def.value : '';
    }

    function setInvalidChoice(invalid, msg) {
      const message = invalid ? (msg || 'Không có lựa chọn này') : '';
      if (invalid) {
        select.dataset.invalidChoice = 'true';
        input.setAttribute('aria-invalid', 'true');
        input.classList.add('is-invalid');
        errorEl.textContent = message;
        errorEl.hidden = false;
      } else {
        delete select.dataset.invalidChoice;
        input.setAttribute('aria-invalid', 'false');
        input.classList.remove('is-invalid');
        errorEl.textContent = '';
        errorEl.hidden = true;
      }
      if (select.id) {
        const extSlot = document.getElementById(select.id + '-error');
        if (extSlot && (invalid || extSlot.textContent === 'Không có lựa chọn này')) {
          extSlot.textContent = message;
          extSlot.hidden = !message;
        }
      }
    }

    function syncFromSelect() {
      input.disabled = select.disabled;
      toggleBtn.disabled = select.disabled;
      const defOpt = getDefaultOption();
      const defVal = defOpt ? defOpt.value : '';
      input.placeholder = defOpt ? cleanSpaces(defOpt.textContent) : (select.dataset.placeholder || 'Gõ để tìm hoặc chọn...');

      const companion = getCompanionInput();
      if (companion && cleanSpaces(companion.value)) {
        const createdText = cleanSpaces(companion.value);
        input.value = createdText;
        clearBtn.hidden = select.disabled;
        return;
      }

      const selectedOpt = select.selectedOptions[0];
      if (!selectedOpt || (defOpt && selectedOpt.value === defVal)) {
        input.value = '';
        clearBtn.hidden = true;
      } else {
        input.value = cleanSpaces(selectedOpt.textContent);
        clearBtn.hidden = select.required || select.disabled;
      }
    }

    function closeListbox() {
      listbox.hidden = true;
      input.setAttribute('aria-expanded', 'false');
      input.removeAttribute('aria-activedescendant');
      activeIndex = -1;
      isFiltering = false;
    }

    function setActive(index) {
      if (!currentItems.length) {
        activeIndex = -1;
        input.removeAttribute('aria-activedescendant');
        return;
      }
      if (index < 0) index = currentItems.length - 1;
      if (index >= currentItems.length) index = 0;
      activeIndex = index;
      currentItems.forEach((li, idx) => {
        const active = idx === activeIndex;
        li.classList.toggle('is-active', active);
        if (active) {
          input.setAttribute('aria-activedescendant', li.id);
          li.scrollIntoView({ block: 'nearest' });
        }
      });
    }

    function selectExistingOption(opt) {
      if (!opt || opt.disabled) return;
      internalMutation = true;
      const companion = getCompanionInput();
      if (companion) companion.value = '';
      [...select.querySelectorAll('option[data-created-option="true"]')].forEach(o => o.remove());
      select.value = opt.value;
      internalMutation = false;

      if (isCheckboxSync && opt.value) {
        const cb = (select.form || document).querySelector(`input[type="checkbox"][name="initialSizeIds"][value="${CSS.escape(opt.value)}"]`);
        if (cb) {
          cb.checked = true;
          cb.dispatchEvent(new Event('change', { bubbles: true }));
        }
      }

      setInvalidChoice(false);
      syncFromSelect();
      closeListbox();
      select.dispatchEvent(new Event('change', { bubbles: true }));
      select.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function selectCreatedValue(rawValue) {
      const cleaned = cleanSpaces(rawValue);
      if (!cleaned) return;
      if (maxLen > 0 && cleaned.length > maxLen) {
        setInvalidChoice(true, `Tối đa ${maxLen} ký tự.`);
        return;
      }
      const defVal = getDefaultEmptyValue();
      const existing = [...select.options].find(o => !o.dataset.createdOption && o.value !== defVal && foldVi(o.textContent) === foldVi(cleaned));
      if (existing && !existing.disabled) {
        selectExistingOption(existing);
        return;
      }

      internalMutation = true;
      const companion = getCompanionInput();
      if (companion) companion.value = cleaned;

      [...select.querySelectorAll('option[data-created-option="true"]')].forEach(o => o.remove());
      const createdOpt = document.createElement('option');
      createdOpt.dataset.createdOption = 'true';
      createdOpt.value = createValueMode === 'text' ? cleaned : '0';
      createdOpt.textContent = cleaned;
      createdOpt.selected = true;
      select.appendChild(createdOpt);
      select.value = createdOpt.value;
      internalMutation = false;

      if (isCheckboxSync) {
        let preview = wrapper.parentElement?.querySelector('[data-created-size-badge]');
        if (!preview) {
          preview = document.createElement('span');
          preview.dataset.createdSizeBadge = 'true';
          preview.className = 'badge bg-primary-subtle text-primary border border-primary-subtle mt-2 d-inline-block';
          wrapper.after(preview);
        }
        preview.textContent = 'Kích cỡ mới sẽ tạo khi lưu: ' + cleaned;
        preview.hidden = false;
      }

      setInvalidChoice(false);
      input.value = cleaned;
      clearBtn.hidden = select.disabled;
      closeListbox();
      select.dispatchEvent(new Event('change', { bubbles: true }));
      select.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function clearSelection() {
      internalMutation = true;
      const companion = getCompanionInput();
      if (companion) companion.value = '';
      [...select.querySelectorAll('option[data-created-option="true"]')].forEach(o => o.remove());
      select.value = getDefaultEmptyValue();
      internalMutation = false;

      const preview = wrapper.parentElement?.querySelector('[data-created-size-badge]');
      if (preview) preview.hidden = true;

      setInvalidChoice(false);
      syncFromSelect();
      closeListbox();
      select.dispatchEvent(new Event('change', { bubbles: true }));
      select.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function renderListbox(filterText) {
      const query = isFiltering ? cleanSpaces(filterText) : '';
      const foldedQuery = foldVi(query);
      const defOpt = getDefaultOption();
      const defVal = defOpt ? defOpt.value : '';

      const options = [...select.options].filter(o => !o.dataset.createdOption);
      const filtered = options.filter(opt => {
        if (select.required && opt.value === '') return false;
        if (!foldedQuery) return true;
        return foldVi(opt.textContent).includes(foldedQuery);
      });

      listbox.replaceChildren();
      currentItems = [];

      filtered.forEach((opt, idx) => {
        const li = document.createElement('li');
        li.className = 'combo-option';
        li.id = `${listboxId}-opt-${idx}`;
        li.setAttribute('role', 'option');
        const isSelected = opt.value === select.value && !getCompanionInput()?.value;
        li.setAttribute('aria-selected', isSelected ? 'true' : 'false');
        if (opt.disabled) {
          li.setAttribute('aria-disabled', 'true');
        }
        li.dataset.value = opt.value;
        li.textContent = cleanSpaces(opt.textContent);
        li.addEventListener('mousedown', (e) => {
          e.preventDefault();
          e.stopPropagation();
          if (!opt.disabled) selectExistingOption(opt);
        });
        listbox.appendChild(li);
        if (!opt.disabled) currentItems.push(li);
      });

      const exactMatch = options.find(
        opt => opt.value !== defVal && foldVi(opt.textContent) === foldedQuery
      );

      if (isCreatable && query.length > 0 && !exactMatch) {
        const createLi = document.createElement('li');
        createLi.className = 'combo-option combo-option-create';
        createLi.id = `${listboxId}-create`;
        createLi.setAttribute('role', 'option');
        createLi.setAttribute('aria-selected', 'false');
        createLi.dataset.createValue = query;
        createLi.textContent = `Thêm mới: ${query}`;
        createLi.addEventListener('mousedown', (e) => {
          e.preventDefault();
          e.stopPropagation();
          selectCreatedValue(query);
        });
        listbox.appendChild(createLi);
        currentItems.push(createLi);
        setInvalidChoice(false);
      } else if (!isCreatable && isFiltering && query.length > 0 && filtered.filter(o => !o.disabled).length === 0) {
        const emptyLi = document.createElement('li');
        emptyLi.className = 'combo-empty';
        emptyLi.setAttribute('role', 'option');
        emptyLi.setAttribute('aria-disabled', 'true');
        emptyLi.textContent = 'Không có lựa chọn này';
        listbox.appendChild(emptyLi);
        setInvalidChoice(true, 'Không có lựa chọn này');
      } else if (isFiltering) {
        setInvalidChoice(false);
      }

      listbox.hidden = false;
      input.setAttribute('aria-expanded', 'true');

      if (currentItems.length > 0) {
        const selectedIdx = currentItems.findIndex(li => li.getAttribute('aria-selected') === 'true');
        setActive(selectedIdx >= 0 ? selectedIdx : 0);
      } else {
        setActive(-1);
      }
    }

    function commitInputOnBlurOrSubmit() {
      if (!isFiltering && select.dataset.invalidChoice !== 'true') return;
      const raw = cleanSpaces(input.value);
      const defOpt = getDefaultOption();
      const defVal = defOpt ? defOpt.value : '';

      if (!raw) {
        if (!select.required) {
          clearSelection();
        } else {
          syncFromSelect();
          setInvalidChoice(false);
        }
        return;
      }

      const options = [...select.options].filter(o => !o.dataset.createdOption && !o.disabled);
      const exact = options.find(o => o.value !== defVal && foldVi(o.textContent) === foldVi(raw));
      if (exact) {
        selectExistingOption(exact);
        return;
      }

      if (isCreatable) {
        selectCreatedValue(raw);
      } else {
        setInvalidChoice(true, 'Không có lựa chọn này');
      }
    }

    input.addEventListener('focus', () => {
      if (input.disabled) return;
      isFiltering = false;
      renderListbox('');
      if (input.value) {
        try { input.select(); } catch (_) {}
      }
    });

    input.addEventListener('click', () => {
      if (input.disabled) return;
      if (listbox.hidden) {
        isFiltering = false;
        renderListbox('');
      }
    });

    input.addEventListener('input', () => {
      if (input.disabled) return;
      isFiltering = true;
      const raw = cleanSpaces(input.value);
      if (!raw && !select.required) {
        clearBtn.hidden = true;
      } else {
        clearBtn.hidden = select.required || select.disabled || !raw;
      }
      renderListbox(input.value);
    });

    input.addEventListener('keydown', (e) => {
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        if (listbox.hidden) {
          isFiltering = false;
          renderListbox('');
        } else {
          setActive(activeIndex + 1);
        }
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        if (listbox.hidden) {
          isFiltering = false;
          renderListbox('');
        } else {
          setActive(activeIndex - 1);
        }
      } else if (e.key === 'Enter') {
        if (!listbox.hidden) {
          e.preventDefault();
          if (activeIndex >= 0 && currentItems[activeIndex]) {
            const item = currentItems[activeIndex];
            if (item.dataset.createValue != null) {
              selectCreatedValue(item.dataset.createValue);
            } else {
              const opt = [...select.options].find(o => o.value === item.dataset.value && !o.dataset.createdOption);
              if (opt) selectExistingOption(opt);
            }
          } else {
            commitInputOnBlurOrSubmit();
          }
        } else if (select.dataset.invalidChoice === 'true') {
          e.preventDefault();
        }
      } else if (e.key === 'Escape') {
        if (!listbox.hidden) {
          e.preventDefault();
          e.stopPropagation();
          closeListbox();
          if (select.dataset.invalidChoice === 'true') {
            setInvalidChoice(false);
            syncFromSelect();
          }
        }
      }
    });

    input.addEventListener('blur', () => {
      setTimeout(() => {
        if (wrapper.contains(document.activeElement)) return;
        commitInputOnBlurOrSubmit();
        closeListbox();
      }, 120);
    });

    clearBtn.addEventListener('mousedown', (e) => {
      e.preventDefault();
      e.stopPropagation();
      clearSelection();
      input.focus();
    });

    toggleBtn.addEventListener('mousedown', (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (input.disabled) return;
      if (listbox.hidden) {
        input.focus();
        isFiltering = false;
        renderListbox('');
      } else {
        closeListbox();
      }
    });

    select.addEventListener('change', () => {
      if (!internalMutation) {
        setInvalidChoice(false);
        syncFromSelect();
      }
    });

    select.addEventListener('combo:sync', () => {
      syncFromSelect();
    });

    const observer = new MutationObserver(() => {
      if (internalMutation) return;
      syncFromSelect();
      if (!listbox.hidden) {
        renderListbox(input.value);
      }
    });
    observer.observe(select, { childList: true, attributes: true, subtree: true, attributeFilter: ['disabled', 'required', 'selected'] });

    select._comboCommit = commitInputOnBlurOrSubmit;
    syncFromSelect();
  }

  function enhanceAll(root = document) {
    root.querySelectorAll('select').forEach(enhanceSelect);
  }

  document.addEventListener('submit', (e) => {
    const form = e.target;
    if (!(form instanceof HTMLFormElement)) return;
    form.querySelectorAll('select[data-combo-enhanced="true"]').forEach(select => {
      select._comboCommit?.();
    });
    const invalidSelect = form.querySelector('select[data-invalid-choice="true"]');
    if (invalidSelect) {
      e.preventDefault();
      e.stopImmediatePropagation();
      invalidSelect._comboInput?.focus();
    }
  }, true);

  document.addEventListener('mousedown', (e) => {
    document.querySelectorAll('.combo-box').forEach(box => {
      if (!box.contains(e.target)) {
        const listbox = box.querySelector('.combo-listbox');
        const input = box.querySelector('.combo-input');
        if (listbox && !listbox.hidden) {
          listbox.hidden = true;
          input?.setAttribute('aria-expanded', 'false');
        }
      }
    });
  });

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => enhanceAll());
  } else {
    enhanceAll();
  }

  const domObserver = new MutationObserver((mutations) => {
    for (const m of mutations) {
      for (const node of m.addedNodes) {
        if (node.nodeType === 1) {
          if (node.tagName === 'SELECT') enhanceSelect(node);
          else if (node.querySelectorAll) enhanceAll(node);
        }
      }
    }
  });
  if (document.documentElement) {
    domObserver.observe(document.documentElement, { childList: true, subtree: true });
  }
})();
