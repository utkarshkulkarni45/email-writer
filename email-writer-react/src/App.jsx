import React, { useState } from "react";
import {
  ThemeProvider,
  createTheme,
  Container,
  Typography,
  Alert,
  Paper,
  Box,
  CssBaseline,
} from "@mui/material";
import EmailInputForm from "./components/EmailInputForm";
import ReplyViewer from "./components/ReplyViewer";
import { generateEmailReply } from "./services/api";
import "./App.css";

const theme = createTheme({
  palette: {
    mode: "light",
    primary: {
      main: "#4f46e5",
      light: "#6366f1",
      dark: "#4338ca",
    },
    background: {
      default: "#f8fafc",
      paper: "#ffffff",
    },
    text: {
      primary: "#0f172a",
      secondary: "#64748b",
    },
  },
  typography: {
    fontFamily: [
      "Inter",
      "-apple-system",
      "BlinkMacSystemFont",
      '"Segoe UI"',
      "Roboto",
      "sans-serif",
    ].join(","),
    button: {
      textTransform: "none",
      fontWeight: 600,
    },
  },
  shape: {
    borderRadius: 12,
  },
});

function App() {
  const [emailContent, setEmailContent] = useState("");
  const [tone, setTone] = useState("professional");
  const [generatedReply, setGeneratedReply] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const reply = await generateEmailReply(emailContent, tone);
      setGeneratedReply(reply);
    } catch (err) {
      if (err.name === "AbortError") {
        setError("The server took too long to respond. The free-tier instance may be waking up. Please retry in 15 seconds.");
      } else {
        setError(err.message || "Failed to generate reply. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Container maxWidth="md" sx={{ px: { xs: 2, sm: 3 }, py: 4 }}>
        <Paper
          elevation={0}
          sx={{
            p: { xs: 3, sm: 5 },
            background: "#ffffff",
            boxShadow: "0 10px 30px -10px rgba(0, 0, 0, 0.08)",
            border: "1px solid rgba(226, 232, 240, 0.8)",
          }}
        >
          {/* Header */}
          <Box sx={{ mb: 4, textAlign: "center" }}>
            <Typography
              variant="h4"
              component="h1"
              sx={{
                fontWeight: 800,
                color: "#0f172a",
                letterSpacing: "-0.03em",
                mb: 1,
              }}
            >
              GmailGenie AI
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 520, mx: "auto" }}>
              Context-aware email response generator powered by Spring Boot and Google Gemini.
            </Typography>
          </Box>

          {error && (
            <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }}>
              {error}
            </Alert>
          )}

          <EmailInputForm
            emailContent={emailContent}
            setEmailContent={setEmailContent}
            tone={tone}
            setTone={setTone}
            onSubmit={handleSubmit}
            loading={loading}
          />

          <ReplyViewer reply={generatedReply} />
        </Paper>

        <Box sx={{ mt: 3, textAlign: "center" }}>
          <Typography variant="caption" color="text.secondary">
            Built by Utkarsh Kulkarni &bull; <a href="https://github.com/utkarshkulkarni45/GmailGenie_AI" target="_blank" rel="noreferrer" style={{ color: "#4f46e5", textDecoration: "none" }}>GitHub Repository</a>
          </Typography>
        </Box>
      </Container>
    </ThemeProvider>
  );
}

export default App;
