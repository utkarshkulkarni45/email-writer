function getEmailContentToReply() {
  // 1. First, check if the user wrote anything in the compose/reply box itself (e.g. rough notes)
  const composeBox =
    document.querySelector('div[contenteditable="true"][role="textbox"]') ||
    document.querySelector(".Am.Al.editable.LW-avf.tS-tW");

  let draftText = "";
  if (composeBox) {
    const cloned = composeBox.cloneNode(true);
    // Remove signatures/quotes inside compose box
    const cleanSelectors = [".gmail_signature", ".gmail_quote", ".gmail_extra", ".h7"];
    cleanSelectors.forEach((sel) => {
      cloned.querySelectorAll(sel).forEach((el) => el.remove());
    });
    draftText = cloned.innerText.trim();
  }

  // If user typed some bullet points or draft notes, use that
  if (draftText && draftText.length > 5) {
    return draftText;
  }

  // 2. Otherwise, look for the received email thread being replied to in the active Gmail view
  const emailBodies = document.querySelectorAll(".a3s.aiL, .a3s.ail");
  if (emailBodies.length > 0) {
    // Get the latest incoming email message in the thread
    const latestEmail = emailBodies[emailBodies.length - 1];
    const emailClone = latestEmail.cloneNode(true);

    // Remove quotes and signatures from the incoming email
    const removeSelectors = [".gmail_quote", ".gmail_extra", ".gmail_signature", ".h7"];
    removeSelectors.forEach((sel) => {
      emailClone.querySelectorAll(sel).forEach((el) => el.remove());
    });

    const receivedText = emailClone.innerText.trim();
    if (receivedText) {
      return receivedText;
    }
  }

  // 3. Fallback: try reading the entire visible email thread text
  const messageBody = document.querySelector(".adn.ads") || document.querySelector(".ii.gt");
  if (messageBody) {
    const text = messageBody.innerText.trim();
    if (text) return text;
  }

  return draftText || "";
}

function injectButton() {
  const existingContainer = document.querySelector(".genie-ai-container");
  if (existingContainer) {
    return;
  }

  // Find the Gmail compose toolbar
  const toolbar =
    document.querySelector(".btC") || document.querySelector(".aDh");

  if (!toolbar) {
    return;
  }

  // Create container for button + tone dropdown
  const container = document.createElement("div");
  container.className = "genie-ai-container";

  // Create tone selector dropdown
  const toneSelect = document.createElement("select");
  toneSelect.className = "genie-tone-select";
  toneSelect.setAttribute("aria-label", "Select tone for reply");
  toneSelect.innerHTML = `
    <option value="professional">Professional</option>
    <option value="friendly">Friendly</option>
    <option value="casual">Casual</option>
    <option value="persuasive">Persuasive</option>
    <option value="urgent">Urgent</option>
  `;

  // Create action button
  const button = document.createElement("button");
  button.textContent = "Generate Reply";
  button.className = "Genie-AI-Button";
  button.setAttribute("type", "button");

  container.appendChild(toneSelect);
  container.appendChild(button);
  toolbar.appendChild(container);

  button.addEventListener("click", async (e) => {
    e.preventDefault();
    const selectedTone = toneSelect.value || "professional";

    try {
      button.textContent = "Generating...";
      button.disabled = true;
      toneSelect.disabled = true;

      // Find Gmail compose editable box
      const composeBox =
        document.querySelector('div[contenteditable="true"][role="textbox"]') ||
        document.querySelector(".Am.Al.editable.LW-avf.tS-tW");

      if (!composeBox) {
        throw new Error("Compose box not found. Please open an email reply box.");
      }

      // Extract the email content to reply to
      const emailContent = getEmailContentToReply();

      if (!emailContent) {
        throw new Error("No incoming message found. Please type a few bullet points in the compose box to reply.");
      }

      if (emailContent.length > 5000) {
        throw new Error("Email content exceeds 5000 characters. Please shorten it before generating.");
      }

      // Call Spring Boot backend (Render Deployment with timeout)
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 45000); // 45s timeout for cold start

      const response = await fetch("https://gmailgenie-ai.onrender.com/api/email/generate", {
        method: "POST",
        signal: controller.signal,
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          emailContent: emailContent,
          tone: selectedTone,
        }),
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        let errorMsg = `Server returned ${response.status}`;
        try {
          const errData = await response.json();
          errorMsg = errData.message || errorMsg;
        } catch {
          const text = await response.text();
          if (text) errorMsg = text;
        }
        throw new Error(errorMsg);
      }

      const generatedReply = await response.text();

      if (!generatedReply || !generatedReply.trim()) {
        throw new Error("Received an empty response from server.");
      }

      // Insert AI generated response into Gmail compose box
      composeBox.focus();
      composeBox.innerText = generatedReply;

      // Dispatch input event so Gmail detects and enables the Send button
      composeBox.dispatchEvent(
        new InputEvent("input", {
          bubbles: true,
          inputType: "insertText",
          data: generatedReply,
        })
      );
    } catch (error) {
      if (error.name === "AbortError") {
        alert("Request timed out. The free backend server is waking up from sleep. Please try again in 15 seconds.");
      } else {
        alert("GmailGenie: " + error.message);
      }
    } finally {
      button.textContent = "Generate Reply";
      button.disabled = false;
      toneSelect.disabled = false;
    }
  });
}

// Observe DOM mutations to detect when Compose or Reply is clicked
const observer = new MutationObserver((mutations) => {
  for (const mutation of mutations) {
    const addedNodes = Array.from(mutation.addedNodes);

    const hasComposeElement = addedNodes.some(
      (node) =>
        node.nodeType === Node.ELEMENT_NODE &&
        (node.matches(".aDh, .btC") ||
          node.querySelector(".aDh, .btC") ||
          node.querySelector('div[role="dialog"]'))
    );

    if (hasComposeElement) {
      setTimeout(injectButton, 400);
    }
  }
});

// Start observing Gmail
observer.observe(document.body, {
  childList: true,
  subtree: true,
});
