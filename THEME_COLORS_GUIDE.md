# Theme Colors Guide ??

Complete guide for all available theme colors in config.json and how to use them in your HTML/CSS/JavaScript.

---

## ?? Available Colors

### All 21 Customizable Colors

| Color Name | Purpose | Example Value | CSS Property |
|------------|---------|---------------|--------------|
| `primary_color` | Main brand color, splash gradient start | `#ff6b9d` | `background`, `color` |
| `secondary_color` | Secondary brand color, splash gradient middle | `#c94b7f` | `background`, `color` |
| `accent_color` | Accent/highlight color, splash gradient end | `#ff1493` | `background`, `border-color` |
| `background_color` | Main page background | `#ffffff` | `background-color` |
| `background_secondary_color` | Cards, sections background | `#f8f9fa` | `background-color` |
| `text_color` | Primary text color | `#333333` | `color` |
| `text_secondary_color` | Secondary/muted text | `#666666` | `color` |
| `text_light_color` | Light/placeholder text | `#999999` | `color` |
| `button_color` | Main button background | `#ff1493` | `background-color` |
| `button_text_color` | Button text color | `#ffffff` | `color` |
| `button_hover_color` | Button hover state | `#c94b7f` | `background-color` (hover) |
| `input_background_color` | Input field background | `#ffffff` | `background-color` |
| `input_border_color` | Input field border | `#e0e0e0` | `border-color` |
| `input_focus_color` | Input focus/active state | `#ff6b9d` | `border-color` (focus) |
| `error_color` | Error messages, validation | `#f44336` | `color`, `background` |
| `success_color` | Success messages | `#4caf50` | `color`, `background` |
| `warning_color` | Warning messages | `#ff9800` | `color`, `background` |
| `info_color` | Info messages | `#2196f3` | `color`, `background` |
| `loader_color` | Loading spinners | `#ff6b9d` | `border-color`, `color` |
| `shadow_color` | Box shadows, overlays | `rgba(0, 0, 0, 0.1)` | `box-shadow` |
| `overlay_color` | Modal/dialog overlays | `rgba(0, 0, 0, 0.5)` | `background-color` |

---

## ?? Usage Examples

### 1. Getting Colors in JavaScript

```javascript
// ?????? ???? ?????? ?? Android
function getThemeColors() {
    try {
        if (typeof Android !== 'undefined' && Android.getThemeColors) {
            const colorsJson = Android.getThemeColors();
            return JSON.parse(colorsJson);
        }
    } catch (e) {
        console.error('Error getting theme colors:', e);
    }
    
    // Fallback colors
    return {
        primaryColor: '#6200EE',
        secondaryColor: '#3700B3',
        // ... other colors
    };
}

// ???????
const colors = getThemeColors();
console.log('Primary Color:', colors.primaryColor);
```

---

### 2. Applying Colors to Body

```javascript
const colors = getThemeColors();

// ????? ??? ???????? ? ??? ????
document.body.style.backgroundColor = colors.backgroundColor;
document.body.style.color = colors.textColor;
```

---

### 3. Gradient Background (like Splash Screen)

```javascript
const colors = getThemeColors();

const element = document.getElementById('header');
element.style.background = `linear-gradient(135deg, 
    ${colors.primaryColor}, 
    ${colors.secondaryColor}, 
    ${colors.accentColor}
)`;
```

---

### 4. Button Styling

```javascript
const colors = getThemeColors();
const button = document.getElementById('myButton');

// ?????? ????
button.style.backgroundColor = colors.buttonColor;
button.style.color = colors.buttonTextColor;
button.style.border = 'none';
button.style.padding = '12px 24px';
button.style.borderRadius = '8px';
button.style.cursor = 'pointer';

// Hover effect
button.addEventListener('mouseenter', function() {
    this.style.backgroundColor = colors.buttonHoverColor;
});
button.addEventListener('mouseleave', function() {
    this.style.backgroundColor = colors.buttonColor;
});
```

---

### 5. Input Field with Focus Effect

```javascript
const colors = getThemeColors();
const input = document.getElementById('myInput');

// ?????? ????
input.style.backgroundColor = colors.inputBackgroundColor;
input.style.borderColor = colors.inputBorderColor;
input.style.color = colors.textColor;
input.style.border = '2px solid';
input.style.padding = '10px';
input.style.borderRadius = '6px';

// Focus state
input.addEventListener('focus', function() {
    this.style.borderColor = colors.inputFocusColor;
    this.style.boxShadow = `0 0 0 3px ${colors.inputFocusColor}33`; // 33 = 20% opacity
});

input.addEventListener('blur', function() {
    this.style.borderColor = colors.inputBorderColor;
    this.style.boxShadow = 'none';
});
```

---

### 6. Alert/Notification Messages

```javascript
const colors = getThemeColors();

// Success Alert
function showSuccess(message) {
    const alert = document.createElement('div');
    alert.textContent = message;
    alert.style.padding = '15px';
    alert.style.borderRadius = '8px';
    alert.style.backgroundColor = `${colors.successColor}22`; // 22 = light opacity
    alert.style.color = colors.successColor;
    alert.style.border = `2px solid ${colors.successColor}`;
    document.body.appendChild(alert);
}

// Error Alert
function showError(message) {
    const alert = document.createElement('div');
    alert.textContent = message;
    alert.style.backgroundColor = `${colors.errorColor}22`;
    alert.style.color = colors.errorColor;
    alert.style.border = `2px solid ${colors.errorColor}`;
    // ... rest of styling
}

// Warning Alert
function showWarning(message) {
    const alert = document.createElement('div');
    alert.style.backgroundColor = `${colors.warningColor}22`;
    alert.style.color = colors.warningColor;
    alert.style.border = `2px solid ${colors.warningColor}`;
    // ...
}

// Info Alert
function showInfo(message) {
    const alert = document.createElement('div');
    alert.style.backgroundColor = `${colors.infoColor}22`;
    alert.style.color = colors.infoColor;
    alert.style.border = `2px solid ${colors.infoColor}`;
    // ...
}
```

---

### 7. Loading Spinner

```javascript
const colors = getThemeColors();

// CSS spinner
const spinner = document.createElement('div');
spinner.style.width = '50px';
spinner.style.height = '50px';
spinner.style.border = `5px solid ${colors.shadowColor}`;
spinner.style.borderTop = `5px solid ${colors.loaderColor}`;
spinner.style.borderRadius = '50%';
spinner.style.animation = 'spin 1s linear infinite';

// CSS animation (?? <style>)
const style = document.createElement('style');
style.textContent = `
    @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
    }
`;
document.head.appendChild(style);
```

---

### 8. Card/Container with Shadow

```javascript
const colors = getThemeColors();

const card = document.getElementById('card');
card.style.backgroundColor = colors.backgroundSecondaryColor;
card.style.padding = '20px';
card.style.borderRadius = '12px';
card.style.boxShadow = `0 4px 12px ${colors.shadowColor}`;
```

---

### 9. Modal Overlay

```javascript
const colors = getThemeColors();

const overlay = document.createElement('div');
overlay.style.position = 'fixed';
overlay.style.top = '0';
overlay.style.left = '0';
overlay.style.width = '100%';
overlay.style.height = '100%';
overlay.style.backgroundColor = colors.overlayColor;
overlay.style.zIndex = '999';
document.body.appendChild(overlay);
```

---

### 10. Complete Theme Application Function

```javascript
function applyTheme() {
    const colors = getThemeColors();
    
    // Document
    document.body.style.backgroundColor = colors.backgroundColor;
    document.body.style.color = colors.textColor;
    
    // All buttons
    document.querySelectorAll('.btn-primary').forEach(btn => {
        btn.style.backgroundColor = colors.buttonColor;
        btn.style.color = colors.buttonTextColor;
    });
    
    // All inputs
    document.querySelectorAll('input, textarea').forEach(input => {
        input.style.backgroundColor = colors.inputBackgroundColor;
        input.style.borderColor = colors.inputBorderColor;
        input.style.color = colors.textColor;
    });
    
    // Secondary text
    document.querySelectorAll('.text-secondary').forEach(el => {
        el.style.color = colors.textSecondaryColor;
    });
    
    // Light text
    document.querySelectorAll('.text-light').forEach(el => {
        el.style.color = colors.textLightColor;
    });
    
    console.log('? Theme applied successfully');
}

// ????? ?? ????? ???
document.addEventListener('DOMContentLoaded', applyTheme);
```

---

## ?? Color Combinations by Flavor

### SexChat Theme (Pink/Purple)

```json
{
  "primary_color": "#ff6b9d",
  "secondary_color": "#c94b7f",
  "accent_color": "#ff1493",
  "button_color": "#ff1493",
  "loader_color": "#ff6b9d"
}
```

**Best For:** Romantic, adult, entertainment apps

---

### mParivahan Theme (Blue)

```json
{
  "primary_color": "#4fc3f7",
  "secondary_color": "#29b6f6",
  "accent_color": "#1976d2",
  "button_color": "#1976d2",
  "loader_color": "#4fc3f7"
}
```

**Best For:** Official, government, professional apps

---

### SexyHub Theme (Pink/Red)

```json
{
  "primary_color": "#f093fb",
  "secondary_color": "#f5576c",
  "accent_color": "#ff006e",
  "button_color": "#ff006e",
  "loader_color": "#f093fb"
}
```

**Best For:** Media, entertainment, content platforms

---

## ?? CSS Custom Properties Alternative

Instead of inline styles, you can use CSS variables:

```javascript
function applyThemeAsCSS() {
    const colors = getThemeColors();
    
    const style = document.createElement('style');
    style.textContent = `
        :root {
            --primary-color: ${colors.primaryColor};
            --secondary-color: ${colors.secondaryColor};
            --accent-color: ${colors.accentColor};
            --bg-color: ${colors.backgroundColor};
            --bg-secondary-color: ${colors.backgroundSecondaryColor};
            --text-color: ${colors.textColor};
            --text-secondary-color: ${colors.textSecondaryColor};
            --text-light-color: ${colors.textLightColor};
            --button-color: ${colors.buttonColor};
            --button-text-color: ${colors.buttonTextColor};
            --button-hover-color: ${colors.buttonHoverColor};
            --input-bg-color: ${colors.inputBackgroundColor};
            --input-border-color: ${colors.inputBorderColor};
            --input-focus-color: ${colors.inputFocusColor};
            --error-color: ${colors.errorColor};
            --success-color: ${colors.successColor};
            --warning-color: ${colors.warningColor};
            --info-color: ${colors.infoColor};
            --loader-color: ${colors.loaderColor};
            --shadow-color: ${colors.shadowColor};
            --overlay-color: ${colors.overlayColor};
        }
    `;
    document.head.appendChild(style);
}
```

Then in your CSS:

```css
body {
    background-color: var(--bg-color);
    color: var(--text-color);
}

.button {
    background-color: var(--button-color);
    color: var(--button-text-color);
}

.button:hover {
    background-color: var(--button-hover-color);
}

.card {
    background-color: var(--bg-secondary-color);
    box-shadow: 0 4px 12px var(--shadow-color);
}

input {
    background-color: var(--input-bg-color);
    border: 2px solid var(--input-border-color);
}

input:focus {
    border-color: var(--input-focus-color);
}

.alert-success {
    background-color: var(--success-color);
}

.alert-error {
    background-color: var(--error-color);
}

.loader {
    border-top-color: var(--loader-color);
}
```

---

## ?? Modifying Colors

To change any color, simply edit the `config.json` file:

**File:** `app/src/sexychat/assets/config.json`

```json
{
  "theme": {
    "button_color": "#ff0000",  // Now buttons are red!
    "success_color": "#00ff00",  // Green success messages
    "loader_color": "#0000ff"    // Blue loaders
  }
}
```

**No rebuild required!** Just reinstall the APK.

---

## ? Best Practices

### 1. Always Provide Fallbacks

```javascript
const colors = getThemeColors() || {
    primaryColor: '#6200EE',
    // ... default colors
};
```

### 2. Use Consistent Naming

Stick to camelCase in JavaScript:
- ? `colors.primaryColor`
- ? `colors.primary_color`

### 3. Add Opacity for Backgrounds

```javascript
// Light background with opacity
element.style.backgroundColor = `${colors.primaryColor}22`; // 22 = ~13%
element.style.backgroundColor = `${colors.primaryColor}44`; // 44 = ~27%
element.style.backgroundColor = `${colors.primaryColor}88`; // 88 = ~53%
```

### 4. Test in Both Light & Dark

Make sure your color combinations work well:
```javascript
// Check contrast
function hasGoodContrast(color1, color2) {
    // Implement contrast ratio check
    // Aim for at least 4.5:1 for normal text
}
```

---

## ?? Testing Theme

Use the example file to test all colors:

**Location:** `app/src/main/assets/theme-example.html`

This file demonstrates:
- ? All 21 colors in action
- ? Interactive elements (buttons, inputs)
- ? Alerts (success, error, warning, info)
- ? Loading spinners
- ? Gradient backgrounds
- ? Hover effects

---

## ?? Related Documentation

- [CONFIG_GUIDE.md](./CONFIG_GUIDE.md) - Complete config management
- [COMPLETE_API_REFERENCE.md](./COMPLETE_API_REFERENCE.md) - API reference
- [FLAVORS_GUIDE.md](./FLAVORS_GUIDE.md) - Build flavors

---

**Last Updated:** 2025-11-01  
**Version:** 1.0
