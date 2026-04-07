var API_BASE = '/api';

document.addEventListener('DOMContentLoaded', function() {
    loadProducts();
    checkLoginStatus();
    loadOrders();

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

    document.getElementById('btn-search').addEventListener('click', function() {
        searchProducts();
    });
    document.getElementById('search-input').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') searchProducts();
    });
    document.getElementById('btn-sync-es').addEventListener('click', function() {
        syncToES();
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
            localStorage.setItem('userId', data.data.userId);
            showMessage('登录成功！', 'success');
            checkLoginStatus();
            loadOrders();
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
    localStorage.removeItem('userId');
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
                    '<button class="btn btn-seckill" onclick="doSeckill(' + p.id + ')">秒杀抢购</button>' +
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

function renderProductCards(products, containerId) {
    var container = document.getElementById(containerId);
    if (products && products.length > 0) {
        container.innerHTML = products.map(function(p) {
            return '<div class="product-card">' +
                '<h3>' + p.name + '</h3>' +
                '<p class="description">' + (p.description || '') + '</p>' +
                '<p class="price">\u00a5' + p.price + '</p>' +
                '<p class="stock">库存: ' + p.stock + '</p>' +
            '</div>';
        }).join('');
    } else {
        container.innerHTML = '<p>暂无数据</p>';
    }
}

function searchProducts() {
    var keyword = document.getElementById('search-input').value.trim();
    fetch(API_BASE + '/search?keyword=' + encodeURIComponent(keyword))
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            renderProductCards(data.data, 'search-results');
        } else {
            document.getElementById('search-results').innerHTML = '<p>' + (data.message || '搜索失败') + '</p>';
        }
    })
    .catch(function() {
        document.getElementById('search-results').innerHTML = '<p>搜索请求失败，请确认ES服务已启动</p>';
    });
}

function syncToES() {
    var btn = document.getElementById('btn-sync-es');
    btn.textContent = '同步中...';
    btn.disabled = true;
    fetch(API_BASE + '/search/sync', { method: 'POST' })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        btn.textContent = '同步数据到ES';
        btn.disabled = false;
        if (data.code === 200) {
            alert('同步成功！现在可以搜索商品了');
        } else {
            alert('同步失败: ' + (data.message || ''));
        }
    })
    .catch(function() {
        btn.textContent = '同步数据到ES';
        btn.disabled = false;
        alert('同步请求失败，请确认ES服务已启动');
    });
}

function doSeckill(productId) {
    var userId = localStorage.getItem('userId');
    if (!userId) {
        alert('请先登录后再参与秒杀');
        return;
    }
    fetch(API_BASE + '/seckill/' + productId + '?userId=' + userId, { method: 'POST' })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            alert('秒杀成功！订单号: ' + data.data.orderId);
            loadProducts();
            loadOrders();
        } else {
            alert(data.message || '秒杀失败');
        }
    })
    .catch(function() {
        alert('网络错误');
    });
}

function loadOrders() {
    var userId = localStorage.getItem('userId');
    if (!userId) return;

    var container = document.getElementById('order-list');
    if (!container) return;

    fetch(API_BASE + '/seckill/orders?userId=' + userId)
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200 && data.data && data.data.length > 0) {
            container.innerHTML = '<table class="order-table"><thead><tr>' +
                '<th>订单号</th><th>商品</th><th>价格</th><th>状态</th><th>时间</th>' +
                '</tr></thead><tbody>' +
                data.data.map(function(o) {
                    var statusText = o.status === 0 ? '待支付' : o.status === 1 ? '已支付' : '已取消';
                    return '<tr><td>' + o.id + '</td><td>' + (o.productName || '') +
                        '</td><td>\u00a5' + o.price + '</td><td>' + statusText +
                        '</td><td>' + (o.createTime || '') + '</td></tr>';
                }).join('') + '</tbody></table>';
        } else {
            container.innerHTML = '<p>暂无订单</p>';
        }
    })
    .catch(function() {
        container.innerHTML = '<p>加载订单失败</p>';
    });
}
