import { useState } from 'react'
import { Box, Button, Container, TextField, Typography, FormControl, InputLabel, Select, MenuItem, CircularProgress } from '@mui/material'
import './App.css'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://smart-email-replier-fakf.onrender.com'

function App() {

  const [emailContent, setEmailContent] = useState('')
  const [tone, setTone] = useState('')
  const [loading, setLoading] = useState(false)
  const [generatedReply ,setGeneratedReply] = useState('')
  const handleSubmit = async () => {
    setLoading(true);

    try {
      const response = await axios.post(`${API_BASE_URL}/api/email/generate`, {
        emailContent,
        tone
      }
      
      );

      setGeneratedReply(typeof response.data == "string" ? response.data : JSON.stringify(response.data));

    } catch (error) {
      console.error("Error generating reply:",error)
      setGeneratedReply("Failed to generate reply. Please check the backend connection and try again.")
    }
    finally{
      setLoading(false)
    }
  }



  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Typography variant='h3' component="h1" gutterBottom>
        Email Reply Generator
      </Typography>

      <Box sx={{ mx: 3, display: "flex", flexDirection: "column", gap: 3, mt: 4 }}>
        <TextField
          fullWidth
          multiline
          rows={6}
          variant="outlined"
          value={emailContent}
          onChange={(e) => setEmailContent(e.target.value)}
          placeholder="Paste the email you want to reply to"
        />
        <FormControl fullWidth>
          <InputLabel id="tone-select-label">Tone (Optional)</InputLabel>
          <Select
            labelId="tone-select-label"
            id="tone-select"
            value={tone}
            label="Tone (Optional)"
            onChange={(e) => setTone(e.target.value)}
          >
            <MenuItem value="None">None</MenuItem>
            <MenuItem value="Professional">Professional</MenuItem>
            <MenuItem value="Casual">Casual</MenuItem>
            <MenuItem value="Friendly">Friendly</MenuItem>
            <MenuItem value="Formal">Formal</MenuItem>
          </Select>

          
        </FormControl>
        <Button
          variant="contained"
          onClick={handleSubmit}
          size="large"
          disabled={!emailContent || loading}
        >
          {loading ? <CircularProgress size={24} color="inherit" /> : 'Generate Reply'}
        </Button>
      </Box>
      
      
      <Box sx={{ mx: 3, display: "flex", flexDirection: "column", gap: 3, mt: 4 }}>
        <TextField
          fullWidth
          multiline
          rows={6}
          variant="outlined"
          value={generatedReply || "Click on generate reply to get the reply"}
          InputProps={{
            readOnly: true,
          }}
        />
        <Button
        variant = "outlined" 
         onClick ={() => navigator.clipboard.write(generatedReply)}>
          Copy to clipboard
         
        </Button>
      </Box>

    </Container>
  )
}

export default App
