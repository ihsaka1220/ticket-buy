// Index page specific code
let currentCategory = '';

// Load events
async function loadEvents(categoryId = '') {
    const eventGrid = document.getElementById('eventGrid');
    eventGrid.innerHTML = '<div class="loading">加载中...</div>';

    const url = categoryId ? `/events?categoryId=${categoryId}` : '/events';
    const response = await api(url);

    if (response && response.code === 200 && response.data) {
        renderEvents(response.data);
    } else {
        eventGrid.innerHTML = '<div class="loading">暂无演出</div>';
    }
}

// Render events
function renderEvents(events) {
    const eventGrid = document.getElementById('eventGrid');

    if (events.length === 0) {
        eventGrid.innerHTML = '<div class="loading">暂无演出</div>';
        return;
    }

    eventGrid.innerHTML = events.map(event => `
        <div class="event-card" onclick="goToDetail(${event.id})">
            <div class="event-image">${getCategoryIcon(event.categoryId)}</div>
            <div class="event-info">
                <span class="event-category">${getCategoryName(event.categoryId)}</span>
                <h3 class="event-title">${event.title}</h3>
                <p class="event-meta">📍 ${event.venue}</p>
                <p class="event-meta">🕐 ${formatDate(event.eventDate)}</p>
                <div class="event-price">
                    <span class="price">¥<span>${getMinPrice(event.id)}</span>起</span>
                    <button class="event-btn" onclick="event.stopPropagation(); goToDetail(${event.id})">立即抢票</button>
                </div>
            </div>
        </div>
    `).join('');
}

// Get category name
function getCategoryName(categoryId) {
    const names = {
        1: '脱口秀',
        2: '音乐会',
        3: '演唱会',
        4: '话剧'
    };
    return names[categoryId] || '演出';
}

// Get minimum price (placeholder - would need to load from ticket types)
let eventPrices = {};
async function loadPrices() {
    // This would normally fetch ticket prices for each event
    // For now, return placeholder
    return 180;
}

function getMinPrice(eventId) {
    const prices = {
        1: 280,
        2: 680,
        3: 280,
        4: 180
    };
    return prices[eventId] || 180;
}

// Go to event detail
function goToDetail(eventId) {
    window.location.href = `detail.html?id=${eventId}`;
}

// Category tab click handler
document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', function() {
        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        this.classList.add('active');
        currentCategory = this.dataset.category;
        loadEvents(currentCategory);
    });
});

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    loadEvents();
});
