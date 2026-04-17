-- ==========================================
-- 购票资格校验Lua脚本
-- Redis Keys:
--   stock:{ticketTypeId} - 库存
--   user:buy:{userId}:{ticketTypeId} - 用户已购买数量
--   user:total:{userId} - 用户总购买数量
-- ==========================================
local stockKey = KEYS[1]
local userBuyKey = KEYS[2]
local userTotalKey = KEYS[3]
local quantity = tonumber(ARGV[1])
local maxPerUser = tonumber(ARGV[2])
local maxTotal = tonumber(ARGV[3])

-- 检查库存
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
    return {-3, '总购买数量超限（最多' .. maxTotal .. '张）'}
end

return {1, '校验通过'}
