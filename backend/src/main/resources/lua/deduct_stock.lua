-- ==========================================
-- 库存扣减Lua脚本
-- ==========================================
local stockKey = KEYS[1]
local userBuyKey = KEYS[2]
local userTotalKey = KEYS[3]
local quantity = tonumber(ARGV[1])
local maxPerUser = tonumber(ARGV[2])
local maxTotal = tonumber(ARGV[3])
local expireTime = ARGV[4]
local orderNo = ARGV[5]

-- 再次检查库存
local stock = tonumber(redis.call('GET', stockKey) or 0)
if stock < quantity then
    return {-1, '库存不足'}
end

-- 检查用户单类型购买数量
local userTypeBuy = tonumber(redis.call('HGET', userBuyKey, 'count') or 0)
if userTypeBuy + quantity > maxPerUser then
    return {-2, '该票档购买数量超限'}
end

-- 检查用户总购买数量
local userTotalBuy = tonumber(redis.call('HGET', userTotalKey, 'count') or 0)
if userTotalBuy + quantity > maxTotal then
    return {-3, '总购买数量超限'}
end

-- 扣减库存
redis.call('DECRBY', stockKey, quantity)

-- 更新用户购买记录
local newUserTypeBuy = userTypeBuy + quantity
redis.call('HSET', userBuyKey, 'count', newUserTypeBuy, 'order', orderNo, 'expire', expireTime)
redis.call('EXPIRE', userBuyKey, 86400)

local newUserTotalBuy = userTotalBuy + quantity
redis.call('HSET', userTotalKey, 'count', newUserTotalBuy)
redis.call('EXPIRE', userTotalKey, 86400)

return {1, '扣减成功', stock - quantity}
