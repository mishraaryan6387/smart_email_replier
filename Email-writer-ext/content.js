console.log("%c Email Writer Assistant Content Script Loaded! ", "background: #222; color: #00ff00; font-size: 20px; font-weight: bold;");

const API_BASE_URL = 'https://smart-email-replier-fakf.onrender.com';

function getEmailContent() {
    const selectors = [
        '.adn.ads .a3s.aiL',
        '.a3s.aiL',
        '.h7',
        '.gmail_quote'
    ];
    for (const selector of selectors) {
        const elements = document.querySelectorAll(selector);
        if (elements.length > 0) {
            // Filter out any elements that might be inside a compose window or dialog
            const validElements = Array.from(elements).filter(el => !el.closest('[role="dialog"], .M9, .btC'));
            const targetElement = validElements.length > 0 ? validElements[validElements.length - 1] : elements[elements.length - 1];
            const content = targetElement.innerText.trim();
            if (content) {
                return content;
            }
        }
    }
    return '';
}

function findComposeToolbar() {
    const selectors = [
        '.btC',
        '.aDh',
        '[role="toolbar"]',
        '.gU.Up'
    ];
    for (const selector of selectors) {
        const toolbar = document.querySelector(selector);
        if (toolbar) {
            return toolbar;
        }
    }
    return null;
}

function createAiButton() {
    const button = document.createElement("div");
    button.className = 'T-I J-J5-Ji AoO v7 T-I-atl L3 ai-reply-button';
    button.innerHTML = 'AI Reply';
    button.setAttribute('role', 'button');
    button.setAttribute('data-tooltip', 'AI Reply');
    button.setAttribute('aria-label', 'AI Reply');
    button.setAttribute('data-tooltip-delay', '450');
    return button;
}

function createToneDropdown() {
    const select = document.createElement("select");
    select.className = 'ai-tone-select';

    const tones = [
        { label: "Auto (Sender's Tone)", value: "matching the sender's tone" },
        { label: "Professional", value: "professional" },
        { label: "Friendly", value: "friendly" },
        { label: "Casual", value: "casual" },
        { label: "Concise", value: "concise" }
    ];

    tones.forEach(t => {
        const option = document.createElement("option");
        option.value = t.value;
        option.textContent = t.label;
        select.appendChild(option);
    });

    return select;
}

function injectButton() {
    const toolbar = findComposeToolbar();
    if (!toolbar) {
        console.log("Toolbar not found");
        return;
    }

    // Remove any existing injected button and tone dropdown to prevent duplicates
    const existingButton = toolbar.querySelector('.ai-reply-button');
    if (existingButton) {
        existingButton.remove();
    }
    const existingDropdown = toolbar.querySelector('.ai-tone-select');
    if (existingDropdown) {
        existingDropdown.remove();
    }

    console.log("Toolbar found:");
    const button = createAiButton();
    const toneDropdown = createToneDropdown();

    // Insert button and tone dropdown at the start of the toolbar
    toolbar.insertBefore(toneDropdown, toolbar.firstChild);
    toolbar.insertBefore(button, toolbar.firstChild);

    button.addEventListener('click', async () => {
        try {
            button.innerHTML = 'Generating...';
            button.style.pointerEvents = 'none';

            const emailContent = getEmailContent();
            const selectedTone = toneDropdown ? toneDropdown.value : "matching the sender's tone";

            const response = await fetch(`${API_BASE_URL}/api/email/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    emailContent: emailContent,
                    tone: selectedTone
                })
            });

            if (!response.ok) {
                throw new Error(`Failed to generate reply: ${response.statusText}`);
            }

            const generatedReply = await response.text();
            const composeBox = document.querySelector(
                '[role="textbox"][g_editable="true"], [role="textbox"][contenteditable="true"], .Am.Al.editable'
            );

            if (composeBox) {
                composeBox.focus();
                // Select all existing text in the compose box so the new reply replaces the previous one
                document.execCommand('selectAll', false, null);
                document.execCommand('insertText', false, generatedReply);
            } else {
                console.error("Compose box not found");
            }
        } catch (error) {
            console.error("Error generating reply:", error);
            alert("Error generating reply: " + error.message);
        } finally {
            button.innerHTML = 'AI Reply';
            button.style.pointerEvents = 'auto';
        }
    });
}

const observer = new MutationObserver((mutations) => {
    for (const mutation of mutations) {
        const addedNodes = Array.from(mutation.addedNodes);
        const hasElement = addedNodes.some(node =>
            node.nodeType === Node.ELEMENT_NODE &&
            (node.matches('.aDh, .btC, [role="dialog"]') || node.querySelector('.aDh, .btC, [role="toolbar"]'))
        );
        if (hasElement) {
            console.log("compose email opened");
            setTimeout(injectButton, 500);
        }
    }
});

observer.observe(document.body, {
    childList: true,
    subtree: true
});