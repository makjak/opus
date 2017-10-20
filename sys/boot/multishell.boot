-- Loads the Opus environment regardless if the file system is local or not
local colors = _G.colors
local fs     = _G.fs
local http   = _G.http
local shell  = _ENV.shell
local term   = _G.term

local w, h = term.getSize()
term.setTextColor(colors.white)
if term.isColor() then
  term.setBackgroundColor(colors.black)
  term.clear()
  local opus = {
    'fffff00',
    'ffff07000',
    'ff00770b00 4444',
    'ff077777444444444',
    'f07777744444444444',
    'f0000777444444444',
    '070000111744444',
    '777770000',
    '7777000000',
    '70700000000',
    '077000000000',
  }
  for k,line in ipairs(opus) do
    term.setCursorPos((w - 18) / 2, k + (h - #opus) / 2)
    term.blit(string.rep(' ', #line), string.rep('a', #line), line)
  end
end

term.setCursorPos((w - 18) / 2, h)
term.write('Loading Opus...')
term.setCursorPos(w, h)

local GIT_REPO = 'kepler155c/opus/develop'
local BASE     = 'https://raw.githubusercontent.com/' .. GIT_REPO

local sandboxEnv = setmetatable({ }, { __index = _G })
for k,v in pairs(_ENV) do
  sandboxEnv[k] = v
end

local function makeEnv()
  local env = setmetatable({ }, { __index = _G })
  for k,v in pairs(sandboxEnv) do
    env[k] = v
  end
  return env
end

-- os.run doesn't provide return values :(
local function run(file, ...)
  local s, m = loadfile(file, makeEnv())
  if s then
    return s(...)
  end
  error('Error loading ' .. file .. '\n' .. m)
end

local function runUrl(file, ...)
  local url = BASE .. '/' .. file

  local u = http.get(url)
  if u then
    local fn = load(u.readAll(), url, nil, makeEnv())
    u.close()
    if fn then
      return fn(...)
    end
  end
  error('Failed to download ' .. url)
end

_G.debug = function() end

-- Install require shim
if fs.exists('sys/apis/injector.lua') then
  _G.requireInjector = run('sys/apis/injector.lua')
else
  -- not local, run the file system directly from git
  _G.requireInjector = runUrl('/sys/apis/injector.lua')
  runUrl('/sys/extensions/vfs.lua')

  -- install file system
  fs.mount('', 'gitfs', GIT_REPO)
end

local Util = run('sys/apis/util.lua')

-- user environment
if not fs.exists('usr/apps') then
  fs.makeDir('usr/apps')
end
if not fs.exists('usr/autorun') then
  fs.makeDir('usr/autorun')
end
if not fs.exists('usr/etc/fstab') or not fs.exists('usr/etc/fstab.ignore') then
  Util.writeFile('usr/etc/fstab', 'usr gitfs kepler155c/opus-apps/develop')
  Util.writeFile('usr/etc/fstab.ignore', 'forced fstab overwrite')
end
if not fs.exists('usr/config/shell') then
  Util.writeTable('usr/config/shell', {
    aliases  = shell.aliases(),
    path     = 'usr/apps:sys/apps:' .. shell.path(),
    lua_path = '/sys/apis:/usr/apis',
  })
end

-- shell environment
local config = Util.readTable('usr/config/shell')
if config.aliases then
  for k in pairs(shell.aliases()) do
    shell.clearAlias(k)
  end
  for k,v in pairs(config.aliases) do
    shell.setAlias(k, v)
  end
end
shell.setPath(config.path)
sandboxEnv.LUA_PATH = config.lua_path

-- extensions
local dir = 'sys/extensions'
for _,file in ipairs(fs.list(dir)) do
  run('sys/apps/shell', fs.combine(dir, file))
end

-- install user file systems
fs.loadTab('usr/etc/fstab')

local args = { ... }
if args[1] then
  term.setBackgroundColor(colors.black)
  term.clear()
  term.setCursorPos(1, 1)
end
args[1] = args[1] or 'sys/apps/multishell'
run('sys/apps/shell', table.unpack(args))

fs.restore()
