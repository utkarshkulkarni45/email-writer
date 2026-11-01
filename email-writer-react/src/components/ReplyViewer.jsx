import React, { useState } from "react";
import { Box, Typography, Paper, Button, Snackbar, Alert, Fade, Chip } from "@mui/material";

export default function ReplyViewer({ reply, modelUsed }) {
  const [copied, setCopied] = useState(false);

  if (!reply) return null;

  const handleCopy = () => {
    navigator.clipboard.writeText(reply);
    setCopied(true);
  };

  return (
    <Fade in={Boolean(reply)} timeout={400}>
      <Box sx={{ mt: 5 }}>
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 1.5 }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
            <Typography variant="h6" fontWeight={700} color="#0f172a" sx={{ fontSize: "1.1rem" }}>
              Generated Response
            </Typography>
            {modelUsed && (
              <Chip label={modelUsed} size="small" variant="outlined" sx={{ color: "#475569" }} />
            )}
          </Box>
          <Button
            variant="outlined"
            size="small"
            onClick={handleCopy}
            sx={{
              borderColor: "#cbd5e1",
              color: "#334155",
              "&:hover": {
                borderColor: "#94a3b8",
                backgroundColor: "#f8fafc",
              },
            }}
          >
            Copy
          </Button>
        </Box>

        <Paper
          elevation={0}
          sx={{
            p: 3,
            whiteSpace: "pre-wrap",
            bgcolor: "#f8fafc",
            border: "1px solid #e2e8f0",
            borderRadius: 3,
            lineHeight: 1.65,
            color: "#1e293b",
            fontSize: "0.95rem",
          }}
        >
          <Typography variant="body1" sx={{ color: "#334155" }}>
            {reply}
          </Typography>
        </Paper>

        <Snackbar
          open={copied}
          autoHideDuration={2500}
          onClose={() => setCopied(false)}
          anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
        >
          <Alert severity="success" variant="filled" onClose={() => setCopied(false)}>
            Copied to clipboard!
          </Alert>
        </Snackbar>
      </Box>
    </Fade>
  );
}
