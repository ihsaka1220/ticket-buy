-- ==========================================
-- 滑动窗口限流Lua脚本
-- ==========================================
local key = KEYS[1]
local window = tonumber(ARGV[1])
local maxCount = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requestId = ARGV[4]

-- 删除窗口外的记录
redis.call('ZREMRANGEBYSCORE', key, 0, now - window * 1000)

-- 获取当前窗口内请求数
local currentCount = redis.call('ZCARD', key)

if currentCount < maxCount then
    -- 添加新请求
    redis.call('ZADD', key, now, requestId)
    redis.call('EXPIRE', key, window)
    return {1, maxCount - currentCount - 1}
else
    return {0, 0}
end
