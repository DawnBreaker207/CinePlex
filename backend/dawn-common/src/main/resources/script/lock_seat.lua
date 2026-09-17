for i, key in ipairs(KEYS) do
    local current = redis.call('GET', key)
    if current then
        local curOwner, curEpoch = string.match(current, '^(.*):(%d+)$')
        local myOwner, myEpoch = string.match(ARGV[1], '^(.*):(%d+)$')
        local sameOwner = curOwner and myOwner and curOwner == myOwner
        local newer = myEpoch and curEpoch and tonumber(myEpoch) > tonumber(curEpoch)
        if not (sameOwner or newer) then
            return { 0, key, current }
        end
    end
end

for i, key in ipairs(KEYS) do
    redis.call('SET', key, ARGV[1], 'EX', ARGV[2])
end

return {1}