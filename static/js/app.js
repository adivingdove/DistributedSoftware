var API_BASE = '/api';

document.addEventListener('DOMContentLoaded', function() {
    loadProducts();
    checkLoginStatus();

    document.getElementById('tab-login').addEventListener('click', function() {
        switchTab('login');
    });
    document.getElementById('tab-register').addEventListener('click', function() {
        switchTab('register');
    });

    document.getElementById('login-form').addEventListener('submit', function(e) {
        e.preventDefault();
        handleLogin();
    });
    document.getElementById('register-form').addEventListener('submit', function(e) {
        e.preventDefault();
        handleRegister();
    });

    document.getElementById('btn-check-server').addEventListener('click', function() {
        checkServer();
    });
});

function switchTab(tab) {
    var tabLogin = document.getElementById('tab-login');
    var tabRegister = document.getElementById('tab-register');
    var loginForm = document.getElementById('login-form');
    var registerForm = document.getElementById('register-form');
    var msg = document.getElementById('auth-msg');

    msg.textContent = '';

    if (tab === 'login') {
        tabLogin.classList.add('active');
        tabRegister.classList.remove('active');
        loginForm.style.display = 'block';
        registerForm.style.display = 'none';
    } else {
        tabLogin.classList.remove('active');
        tabRegister.classList.add('active');
        loginForm.style.display = 'none';
        registerForm.style.display = 'block';
    }
}

function showMessage(text, type) {
    var msg = document.getElementById('auth-msg');
    msg.className = 'message ' + type;
    msg.textContent = text;
}

function handleLogin() {
    var username = document.getElementById('login-username').value.trim();
    var password = document.getElementById('login-password').value.trim();

    if (!username || !password) {
        showMessage('请填写用户名和密码', 'error');
        return;
    }

    fetch(API_BASE + '/user/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username, password: password })
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            localStorage.setItem('token', data.data.token);
            localStorage.setItem('username', data.data.username);
            showMessage('登录成功！', 'success');
            checkLoginStatus();
        } else {
            showMessage(data.message || '登录失败', 'error');
        }
    })
    .catch(function(err) {
        showMessage('网络错误，请稍后重试', 'error');
    });
}

function handleRegister() {
    var username = document.getElementById('reg-username').value.trim();
    var password = document.getElementById('reg-password').value.trim();
    var phone = document.getElementById('reg-phone').value.trim();

    if (!username || !password || !phone) {
        showMessage('请填写所有字段', 'error');
        return;
    }

    fetch(API_BASE + '/user/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username, password: password, phone: phone })
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            showMessage('注册成功！请登录', 'success');
            document.getElementById('reg-username').value = '';
            document.getElementById('reg-password').value = '';
            document.getElementById('reg-phone').value = '';
            setTimeout(function() { switchTab('login'); }, 1000);
        } else {
            showMessage(data.message || '注册失败', 'error');
        }
    })
    .catch(function(err) {
        showMessage('网络错误，请稍后重试', 'error');
    });
}

function checkLoginStatus() {
    var username = localStorage.getItem('username');
    var info = document.getElementById('user-info');
    var authSection = document.getElementById('auth-section');
    if (username) {
        info.innerHTML = '欢迎, ' + username + ' | <a href="#" id="logout-link" style="color:white">退出</a>';
        document.getElementById('logout-link').addEventListener('click', function(e) {
            e.preventDefault();
            logout();
        });
        authSection.innerHTML = '<p style="text-align:center;color:#28a745;font-size:16px;">已登录为 <strong>' + username + '</strong></p>';
    } else {
        info.textContent = '';
        authSection.style.display = '';
    }
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    location.reload();
}

function loadProducts() {
    fetch(API_BASE + '/product/list')
    .then(function(res) { return res.json(); })
    .then(function(data) {
        var container = document.getElementById('product-list');
        if (data.code === 200 && data.data && data.data.length > 0) {
            container.innerHTML = data.data.map(function(p) {
                return '<div class="product-card">' +
                    '<h3>' + p.name + '</h3>' +
                    '<p class="description">' + (p.description || '') + '</p>' +
                    '<p class="price">\u00a5' + p.price + '</p>' +
                    '<p class="stock">库存: ' + p.stock + '</p>' +
                '</div>';
            }).join('');
        } else {
            container.innerHTML = '<p>暂无商品数据</p>';
        }
    })
    .catch(function() {
        document.getElementById('product-list').innerHTML = '<p>加载商品失败</p>';
    });
}

function checkServer() {
    fetch(API_BASE + '/product/port')
    .then(function(res) { return res.json(); })
    .then(function(data) {
        document.getElementById('server-port').textContent = data.data || '未知';
    })
    .catch(function() {
        document.getElementById('server-port').textContent = '请求失败';
    });
}
