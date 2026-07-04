import os
import re

screens_dir = os.path.join(os.path.dirname(__file__), "app", "src", "main", "java", "com", "example", "myapplication", "ui", "screens")

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Skip LoginScreen as it was already handled manually
    if "LoginScreen.kt" in filepath:
        return

    # Add dark mode variable if not present
    if "val isDarkTheme =" not in content and "@Composable\nfun " in content:
        # Find the first @Composable fun that looks like a screen (e.g. HomeScreen, ProfileScreen)
        pattern = r"(@Composable\s+(?:@OptIn[^\n]+\s+)?fun\s+[A-Z]\w*Screen[^{]*\{)"
        replacement = r"\1\n    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()\n    val bgColor = if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF121212) else androidx.compose.ui.graphics.Color.White\n    val surfaceColor = if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF1E1E1E) else androidx.compose.ui.graphics.Color.White\n    val textColor = if (isDarkTheme) androidx.compose.ui.graphics.Color.White else FastPayNavy\n    val secondaryTextColor = if (isDarkTheme) androidx.compose.ui.graphics.Color.LightGray else androidx.compose.ui.graphics.Color.Gray\n"
        content = re.sub(pattern, replacement, content, count=1)

    # Some replacements to use these variables. This needs to be careful.
    # Replace specific color usages in modifiers or texts.
    
    # We will replace 'containerColor = FastPaySurface' with 'containerColor = bgColor' (HomeScreen uses this)
    content = re.sub(r'containerColor\s*=\s*FastPaySurface', 'containerColor = bgColor', content)
    content = re.sub(r'background\(Color\.White\)', 'background(bgColor)', content)
    content = re.sub(r'containerColor\s*=\s*Color\.White', 'containerColor = surfaceColor', content)
    
    # Texts
    content = re.sub(r'color\s*=\s*FastPayNavy\b', 'color = textColor', content)
    content = re.sub(r'tint\s*=\s*FastPayNavy\b', 'tint = textColor', content)
    content = re.sub(r'color\s*=\s*Color\.Gray\b', 'color = secondaryTextColor', content)

    # Note: this is a heuristic approach. It's safe because it uses local variables we just defined.
    # Wait, if we replace in other @Composable functions inside the same file that don't have these variables defined, it will break compilation!
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

for filename in os.listdir(screens_dir):
    if filename.endswith("Screen.kt"):
        process_file(os.path.join(screens_dir, filename))
print("Done")
