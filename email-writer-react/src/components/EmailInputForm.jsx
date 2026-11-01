import React from "react";
import {
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  CircularProgress,
  Box,
} from "@mui/material";

export default function EmailInputForm({
  emailContent,
  setEmailContent,
  tone,
  setTone,
  onSubmit,
  loading,
}) {
  return (
    <Box
      component="form"
      onSubmit={onSubmit}
      sx={{ display: "flex", flexDirection: "column", gap: 2.5 }}
    >
      <TextField
        fullWidth
        label="Original Email Content"
        multiline
        rows={6}
        value={emailContent}
        onChange={(e) => setEmailContent(e.target.value)}
        placeholder="Paste the email you received here..."
        required
        variant="outlined"
        helperText={`${emailContent.length} / 5000 characters`}
        slotProps={{ htmlInput: { maxLength: 5000 } }}
        sx={{
          "& .MuiOutlinedInput-root": {
            backgroundColor: "#fcfcfd",
            "&:hover fieldset": {
              borderColor: "#a5b4fc",
            },
            "&.Mui-focused fieldset": {
              borderColor: "#4f46e5",
            },
          },
        }}
      />

      <FormControl fullWidth>
        <InputLabel id="tone-label">Select Tone (Optional)</InputLabel>
        <Select
          labelId="tone-label"
          value={tone}
          label="Select Tone (Optional)"
          onChange={(e) => setTone(e.target.value)}
          sx={{
            backgroundColor: "#fcfcfd",
            "& .MuiOutlinedInput-notchedOutline": {
              borderColor: "#e2e8f0",
            },
          }}
        >
          <MenuItem value="">Default / Standard</MenuItem>
          <MenuItem value="professional">Professional</MenuItem>
          <MenuItem value="friendly">Friendly</MenuItem>
          <MenuItem value="casual">Casual</MenuItem>
          <MenuItem value="persuasive">Persuasive</MenuItem>
          <MenuItem value="urgent">Urgent and Direct</MenuItem>
        </Select>
      </FormControl>

      <Button
        type="submit"
        variant="contained"
        disabled={loading || !emailContent.trim()}
        sx={{
          py: 1.5,
          fontSize: "1rem",
          fontWeight: 700,
          background: "linear-gradient(135deg, #4f46e5 0%, #6366f1 100%)",
          color: "#ffffff",
          "&:hover": {
            background: "linear-gradient(135deg, #4338ca 0%, #4f46e5 100%)",
          },
        }}
      >
        {loading ? (
          <CircularProgress size={24} sx={{ color: "#ffffff" }} />
        ) : (
          "Generate Reply"
        )}
      </Button>
    </Box>
  );
}
