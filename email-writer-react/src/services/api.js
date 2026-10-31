const API_BASE_URL =
  import.meta.env.VITE_API_URL || "https://gmailgenie-ai.onrender.com/api/email";

export const generateEmailReply = async (emailContent, tone) => {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 45000); // 45s for free tier wake up

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

    clearTimeout(timeoutId);

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

    return await response.text();
  } catch (error) {
    clearTimeout(timeoutId);
    throw error;
  }
};
