// Orders page specific code

// Load user orders
async function loadOrders() {
    const token = getToken();
    if (!token) {
        showLoginModal();
        document.getElementById('orderList').innerHTML = '<div class="empty-orders">请先登录查看订单</div>';
        return;
    }

    const orderList = document.getElementById('orderList');
    orderList.innerHTML = '<div class="loading">加载中...</div>';

    const response = await api('/orders');
    if (response && response.code === 200 && response.data) {
        renderOrders(response.data);
    } else {
        orderList.innerHTML = '<div class="empty-orders">加载失败</div>';
    }
}

// Render orders
function renderOrders(orders) {
    const orderList = document.getElementById('orderList');

    if (orders.length === 0) {
        orderList.innerHTML = `
            <div class="empty-orders">
                <div>📋</div>
                <p>暂无订单</p>
                <a href="index.html" class="btn btn-primary" style="margin-top: 20px;">去抢票</a>
            </div>
        `;
        return;
    }

    orderList.innerHTML = orders.map(order => `
        <div class="order-card">
            <div class="order-header">
                <span class="order-no">订单号: ${order.orderNo}</span>
                <span class="status-badge ${getStatusClass(order.status)}">${getStatusText(order.status)}</span>
            </div>
            <div class="order-content">
                <div class="order-event-info">
                    <h3 class="order-event-title">演出ID: ${order.eventId}</h3>
                    <p class="order-ticket-info">票档ID: ${order.ticketTypeId}</p>
                    <p class="order-ticket-info">数量: ${order.quantity} 张</p>
                    <p class="order-time">下单时间: ${formatDate(order.createdAt)}</p>
                    ${order.status === 1 ? `<p class="order-time" style="color: #dc3545;">过期时间: ${formatDate(order.expireTime)}</p>` : ''}
                </div>
                <div class="order-amount">
                    <div class="order-total">¥<span>${formatPrice(order.totalAmount)}</span></div>
                    ${getOrderActions(order)}
                </div>
            </div>
        </div>
    `).join('');
}

// Get status class
function getStatusClass(status) {
    const classes = {
        1: 'status-pending',
        2: 'status-paid',
        3: 'status-cancelled',
        4: 'status-cancelled',
        5: 'status-closed'
    };
    return classes[status] || 'status-pending';
}

// Get status text
function getStatusText(status) {
    const texts = {
        1: '待支付',
        2: '已支付',
        3: '已取消',
        4: '已退款',
        5: '已关闭'
    };
    return texts[status] || '未知';
}

// Get order actions
function getOrderActions(order) {
    if (order.status === 1) {
        return `
            <div class="order-actions">
                <button class="btn btn-success btn-sm" onclick="payOrder('${order.orderNo}')">去支付</button>
                <button class="btn btn-secondary btn-sm" onclick="cancelOrder('${order.orderNo}')">取消订单</button>
            </div>
        `;
    }
    return '';
}

// Pay order
function payOrder(orderNo) {
    window.location.href = `/api/pay/create/${orderNo}`;
}

// Cancel order
async function cancelOrder(orderNo) {
    if (!confirm('确定要取消这个订单吗？')) {
        return;
    }

    const response = await api(`/orders/${orderNo}/cancel`, {
        method: 'POST'
    });

    if (response && response.code === 200) {
        showToast('订单已取消');
        loadOrders();
    } else {
        showToast(response?.message || '取消失败', 'error');
    }
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    loadOrders();
});
