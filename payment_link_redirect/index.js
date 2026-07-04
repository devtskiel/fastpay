const express = require('express')
const app = express()
const PORT = process.env.PORT || 3000

// Example usage:
// Maya redirect -> https://your-redirect.example/return?linkId=PLK_123&status=SUCCESS
// This server will forward to app deep link: myapp://payment/success?linkId=PLK_123&status=SUCCESS

app.get('/return', (req, res) => {
  const { linkId, status, ...rest } = req.query
  const basePath = status === 'SUCCESS' ? 'myapp://payment/success' : (status === 'CANCELLED' ? 'myapp://payment/cancel' : 'myapp://payment/failure')
  const params = new URLSearchParams(req.query).toString()
  const deepLink = `${basePath}?${params}`

  // Try to redirect to app deep link. If app not installed, render a small page with fallback links.
  const html = `<!doctype html>
  <html>
    <head>
      <meta charset="utf-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1" />
      <title>Returning to App</title>
      <script>
        // Attempt immediate deep link
        window.location = '${deepLink}';
        // After 1.5s, show fallback
        setTimeout(function(){
          document.getElementById('fallback').style.display = 'block'
        }, 1500);
      </script>
      <style> body { font-family: Arial, sans-serif; padding: 2rem; } #fallback { display:none; margin-top:1rem; } a.button{ background:#00C389; color:white; padding:10px 16px; border-radius:6px; text-decoration:none; }</style>
    </head>
    <body>
      <h2>Returning to app…</h2>
      <p>If you are not redirected automatically, use the button below.</p>
      <div id="fallback">
        <a class="button" href="${deepLink}">Open App</a>
        <p style="margin-top:1rem; font-size:0.9rem; color:#666">Or copy this link: <br/><input style="width:100%" value="${deepLink}" readonly /></p>
      </div>
    </body>
  </html>`

  res.send(html)
})

app.get('/', (req, res) => {
  res.send('Payment Link Redirect Helper. Use /return to accept Maya redirects and forward to app deep link.')
})

app.listen(PORT, () => console.log(`Payment redirect helper running on port ${PORT}`))

