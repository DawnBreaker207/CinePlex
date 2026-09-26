for i, key in ipairs(KEYS) do
    local current = redis.call('GET', key)
    if current then
        -- Owner-only refresh: the epoch (System.currentTimeMillis, fresh on
        -- every call) must NEVER let a different owner steal the lock.
        -- A newer epoch from another reservation used to silently take over
        -- seats another user was holding (proven by e2e race test).
        local curOwner = string.match(current, '^(.*):(%d+)$')
        local myOwner = string.match(ARGV[1], '^(.*):(%d+)$')
        local sameOwner = curOwner and myOwner and curOwner == myOwner
        if not sameOwner then
            return { 0, key, current }
        end
    end
end

for i, key in ipairs(KEYS) do
    redis.call('SET', key, ARGV[1], 'EX', ARGV[2])
end

return {1}