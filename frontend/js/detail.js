// Detail page specific code
let eventId = null;
let eventDetail = null;
let selectedTicketType = null;
let quantity = 1;
const maxQuantity = 3;

// Get event ID from URL
function getEventId() {
    const params = new URLSearchParams(window.location.search);
    return params.get('id');
}

// Load event detail
async function loadEventDetail() {
    eventId = getEventId();
    if (!eventId) {
        showToast('演出不存在', 'error');
        window.location.href = 'index.html';
        return;
    }

    const response = await api(`/events/${eventId}`);
    if (response && response.code === 200 && response.data) {
        eventDetail = response.data;
        renderEventDetail();
        renderTicketTypes();
    } else {
        showToast('加载失败', 'error');
    }
}

// Render event detail
function renderEventDetail() {
    const detailDiv = document.getElementById('eventDetail');
    detailDiv.innerHTML = `
        <div class="event-header">
            <div class="event-poster">${getCategoryIcon(eventDetail.categoryId)}</div>
            <div class="event-main-info">
                <span class="event-category-tag">${eventDetail.categoryName}</span>
                <h1 class="event-title">${eventDetail.title}</h1>
                <ul class="event-info-list">
                    <li>
                        <span class="label">场馆</span>
                        <span class="value">${eventDetail.venue}</span>
                    </li>
                    <li>
                        <span class="label">地址</span>
                        <span class="value">${eventDetail.address || '待定'}</span>
                    </li>
                    <li>
                        <span class="label">时间</span>
                        <span class="value">${formatDate(eventDetail.eventDate)}</span>
                    </li>
                    <li>
                        <span class="label">开售</span>
                        <span class="value">${formatDate(eventDetail.saleStartTime)}</span>
                    </li>
                    <li>
                        <span class="label">停售</span>
                        <span class="value">${formatDate(eventDetail.saleEndTime)}</span>
                    </li>
                </ul>
            </div>
        </div>
        <div class="event-description">
            <h3>演出介绍</h3>
            <p>${eventDetail.description || '暂无介绍'}</p>
        </div>
    `;
}

// Render ticket types
function renderTicketTypes() {
    const selectionDiv = document.getElementById('ticketSelection');

    if (!eventDetail.ticketTypes || eventDetail.ticketTypes.length === 0) {
        selectionDiv.innerHTML = '<h2>选择票档</h2><p>暂无可购票档</p>';
        return;
    }

    let html = '<h2>选择票档</h2><div class="ticket-type-list">';

    eventDetail.ticketTypes.forEach((ticket, index) => {
        const stockClass = ticket.availableStock < 50 ? 'low' : '';
        html += `
            <div class="ticket-type-item ${index === 0 ? 'selected' : ''}"
                 data-id="${ticket.id}"
                 onclick="selectTicketType(${ticket.id})">
                <div class="ticket-info">
                    <div class="ticket-name">${ticket.name}</div>
                    <div class="ticket-desc">${ticket.description || ''}</div>
                    <div class="ticket-stock ${stockClass}">
                        剩余 ${ticket.availableStock} 张
                    </div>
                </div>
                <div class="ticket-price-info">
                    <div class="ticket-price">¥<span>${formatPrice(ticket.price)}</span></div>
                </div>
            </div>
        `;
    });

    html += '</div>';
    selectionDiv.innerHTML = html;

    // Select first ticket type by default
    if (eventDetail.ticketTypes.length > 0) {
        selectedTicketType = eventDetail.ticketTypes[0];
    }
}

// Select ticket type
function selectTicketType(ticketTypeId) {
    document.querySelectorAll('.ticket-type-item').forEach(item => {
        item.classList.remove('selected');
        if (parseInt(item.dataset.id) === ticketTypeId) {
            item.classList.add('selected');
        }
    });

    selectedTicketType = eventDetail.ticketTypes.find(t => t.id === ticketTypeId);
}

// Quantity controls
function increaseQty() {
    if (quantity < maxQuantity) {
        quantity++;
        document.getElementById('quantity').value = quantity;
    }
}

function decreaseQty() {
    if (quantity > 1) {
        quantity--;
        document.getElementById('quantity').value = quantity;
    }
}

// Buy ticket
async function buyTicket() {
    const token = getToken();
    if (!token) {
        showLoginModal();
        showToast('请先登录', 'warning');
        return;
    }

    if (!selectedTicketType) {
        showToast('请选择票档', 'warning');
        return;
    }

    if (selectedTicketType.availableStock < quantity) {
        showToast('库存不足', 'error');
        return;
    }

    const buyBtn = document.getElementById('buyBtn');
    buyBtn.disabled = true;
    buyBtn.textContent = '处理中...';

    try {
        const response = await api('/events/buy', {
            method: 'POST',
            body: JSON.stringify({
                eventId: parseInt(eventId),
                ticketTypeId: selectedTicketType.id,
                quantity: quantity
            })
        });

        if (response && response.code === 200) {
            showToast('抢票成功！正在跳转支付...');
            // Store order info and redirect to payment
            setTimeout(() => {
                window.location.href = `/api/pay/create/${response.data.orderNo}`;
            }, 1000);
        } else {
            showToast(response?.message || '抢票失败', 'error');
            buyBtn.disabled = false;
            buyBtn.textContent = '立即购买';
        }
    } catch (error) {
        showToast('网络错误，请重试', 'error');
        buyBtn.disabled = false;
        buyBtn.textContent = '立即购买';
    }
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    loadEventDetail();
});
