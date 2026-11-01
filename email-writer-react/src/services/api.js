const API_BASE_URL =
  import.meta.env.VITE_API_URL || "https://gmailgenie-ai.onrender.com/api/email";

export const generateEmailReply = async (emailContent, tone) => {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 60000); // free tier can take ~30s to wake up

  try {
    const response = await fetch(`${API_BASE_URL}/generate`, {
      method: "POST",
      signal: controller.signal,
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        emailContent,
        tone: tone || "professional",
      }),
    });

    if (!response.ok) {
      let errorMsg = `Server error (${response.status})`;
      try {
        const errorJson = await response.json();
        errorMsg = errorJson.message || errorMsg;
      } catch {
        const text = await response.text();
        if (text) errorMsg = text;
      }
      throw new Error(errorMsg);
    }

    return {
      reply: await response.text(),
      modelUsed: response.headers.get("X-Model-Used"),
    };
  } finally {
    clearTimeout(timeoutId);
  }
};
