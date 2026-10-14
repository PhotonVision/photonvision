/**
 * Photon SC App - Frontend Tab Dashboard
 * 
 * This script manages the dynamic tab interface for loading external web content
 * in iframes. It handles:
 * - Fetching the tab list from the backend
 * - Rendering tab buttons
 * - Loading content in the iframe
 */

const tabList = document.getElementById('tabList');
const tabFrame = document.getElementById('tabFrame');
const refreshButton = document.getElementById('refreshButton');
let tabs = [];
let activeTabIndex = 0;

/**
 * Fetch the list of tabs from the backend.
 * 
 * Called on page load and when user clicks the "Update Clients" button.
 * The /tabs endpoint returns a JSON array of tab objects:
 * [
 *   {"title": "Example", "url": "https://example.com"},
 *   ...
 * ]
 */
async function fetchTabs() {
  try {
    const response = await fetch('/tabs');
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const payload = await response.json();
    tabs = Array.isArray(payload.tabs) ? payload.tabs : [];
    renderTabs();

    // Automatically load the first tab if available
    if (tabs.length > 0) {
      setActiveTab(0);
    } else {
      tabList.innerHTML = '<div class="empty-state">No clients found</div>';
      tabFrame.src = 'about:blank';
    }
  } catch (error) {
    console.error('Failed to load tabs:', error);
    tabList.innerHTML = '<div class="empty-state">Unable to load tabs</div>';
    tabFrame.src = 'about:blank';
  }
}

/**
 * Render tab buttons based on the current tabs list.
 * 
 * Creates a button for each tab in the tabs array and adds click handlers.
 * The buttons are styled with CSS and the active one is highlighted.
 */
function renderTabs() {
  tabList.innerHTML = '';

  tabs.forEach((tab, index) => {
    const button = document.createElement('button');
    button.className = 'tab-button';
    button.textContent = tab.title || `Tab ${index + 1}`;
    // Click handler to switch to this tab
    button.addEventListener('click', () => setActiveTab(index));
    tabList.appendChild(button);
  });

  // Update visual highlighting of the active tab
  updateActiveTabStyles();
}

/**
 * Set the active tab and load its content.
 * 
 * This is the main function that handles tab switching:
 * 1. Validates the index is in range
 * 2. Gets the URL from the selected tab
 * 3. Loads the URL in the iframe
 * 4. Updates visual styling
 * 
 * @param {number} index - Index of tab to activate
 */
function setActiveTab(index) {
  if (index < 0 || index >= tabs.length) {
    return;
  }

  activeTabIndex = index;
  const tab = tabs[index];
  tabFrame.src = tab.url || 'about:blank';
  
  updateActiveTabStyles();


/**
 * Update visual styling to highlight the active tab.
 * 
 * Adds the 'active' CSS class to the selected tab button and removes it
 * from others. The CSS provides visual highlighting (different background
 * color, border color, etc.).
 */
function updateActiveTabStyles() {
  const buttons = tabList.querySelectorAll('.tab-button');
  buttons.forEach((button, index) => {
    button.classList.toggle('active', index === activeTabIndex);
  });
}

// Event listeners
refreshButton.addEventListener('click', fetchTabs);  // "Update Clients" button
window.addEventListener('load', fetchTabs);          // Load tabs on page load
