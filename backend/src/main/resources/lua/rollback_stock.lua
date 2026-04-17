-- ==========================================
-- 库存回滚Lua脚本
-- ==========================================
local stockKey = KEYS[1]
local userBuyKey = KEYS[2]
local userTotalKey = KEYS[3]
local quantity = tonumber(ARGV[1])
local orderNo = ARGV[2]

-- 回滚库存
redis.call('INCRBY', stockKey, quantity)

-- 回滚用户购买记录
local userTypeBuy = tonumber(redis.call('HGET', userBuyKey, 'count') or 0)
redis.call('HSET', userBuyKey, 'count', math.max(0, userTypeBuy - quantity))

local userTotalBuy = tonumber(redis.call('HGET', userTotalKey, 'count') or 0)
redis.call('HSET', userTotalKey, 'count', math.max(0, userTotalBuy - quantity))

return {1, '回滚成功'}
