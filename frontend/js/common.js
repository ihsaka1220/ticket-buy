// Common utilities
const API_BASE = '/api';

// Token management
function getToken() {
    return localStorage.getItem('token');
}

function setToken(token) {
    localStorage.setItem('token', token);
}

function removeToken() {
    localStorage.removeItem('token');
}

// API request helper
async function api(url, options = {}) {
    const token = getToken();
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE}${url}`, {
        ...options,
        headers
    });

    const data = await response.json();

    if (data.code === 401) {
        removeToken();
        updateUserArea();
        showToast('请先登录', 'warning');
        return null;
    }

    return data;
}

// Toast notification
function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.remove();
    }, 3000);
}

// Modal functions
function showLoginModal() {
    document.getElementById('loginModal').classList.add('active');
    document.getElementById('registerModal')?.classList.remove('active');
}

function hideLoginModal() {
    document.getElementById('loginModal').classList.remove('active');
}

function showRegisterModal() {
    document.getElementById('registerModal').classList.add('active');
    document.getElementById('loginModal').classList.remove('active');
}

function hideRegisterModal() {
    document.getElementById('registerModal').classList.remove('active');
}

// Update user area display
async function updateUserArea() {
    const userArea = document.getElementById('userArea');
    const token = getToken();

    if (!token) {
        userArea.innerHTML = '<button class="btn btn-primary" onclick="showLoginModal()">登录</button>';
        return;
    }

    const response = await api('/user/info');
    if (response && response.code === 200 && response.data) {
        userArea.innerHTML = `
            <div class="user-info">
                <span>${response.data.username}</span>
                <button class="btn btn-secondary btn-sm" onclick="logout()">退出</button>
            </div>
        `;
    } else {
        userArea.innerHTML = '<button class="btn btn-primary" onclick="showLoginModal()">登录</button>';
    }
}

// Login form handler
document.getElementById('loginForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;

    const response = await api('/user/login', {
        method: 'POST',
        body: JSON.stringify({ username, password })
    });

    if (response && response.code === 200) {
        setToken(response.data.token);
        hideLoginModal();
        updateUserArea();
        showToast('登录成功');
    } else {
        showToast(response?.message || '登录失败', 'error');
    }
});

// Register form handler
document.getElementById('registerForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('regUsername').value;
    const password = document.getElementById('regPassword').value;
    const phone = document.getElementById('regPhone').value;

    const response = await api('/user/register', {
        method: 'POST',
        body: JSON.stringify({ username, password, phone })
    });

    if (response && response.code === 200) {
        hideRegisterModal();
        showLoginModal();
        showToast('注册成功，请登录');
    } else {
        showToast(response?.message || '注册失败', 'error');
    }
});

// Logout
function logout() {
    api('/user/logout', { method: 'POST' });
    removeToken();
    updateUserArea();
    showToast('已退出登录');
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    updateUserArea();
});

// Category icons
const categoryIcons = {
    1: '🎤', // 脱口秀
    2: '🎵', // 音乐会
    3: '🎸', // 演唱会
    4: '🎭'  // 话剧
};

function getCategoryIcon(categoryId) {
    return categoryIcons[categoryId] || '🎪';
}

// Format date
function formatDate(dateStr) {
    const date = new Date(dateStr);
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Format price
function formatPrice(price) {
    return parseFloat(price).toFixed(2);
}
