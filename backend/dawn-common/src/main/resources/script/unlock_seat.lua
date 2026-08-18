local current = redis.call('GET', KEYS[1])
if not current then
    return 0
end
if current == ARGV[1] or string.sub(current, 1, #ARGV[1] + 1) == ARGV[1] .. ':' then
    return redis.call('DEL', KEYS[1])
end
return 0