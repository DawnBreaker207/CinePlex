for i, key in ipairs(KEYS) do
    local currentOwner = redis.call('GET', key)
    if currentOwner and currentOwner ~= ARGV[1] then
        return { 0, key, currentOwner }
    end
end

for i, key in ipairs(KEYS) do
    redis.call('SET', key, ARGV[1], 'EX', ARGV[2])
end

return {1}