local Util = require('util')

local fs        = _G.fs
local textutils = _G.textutils

local PACKAGE_DIR = 'packages'

local Packages = { }

function Packages:installed()
	local list = { }

	if fs.exists(PACKAGE_DIR) then
		for _, dir in pairs(fs.list(PACKAGE_DIR)) do
			local path = fs.combine(fs.combine(PACKAGE_DIR, dir), '.package')
			list[dir] = Util.readTable(path)
		end
	end

	return list
end

function Packages:list()
	return Util.readTable('usr/config/packages') or { }
end

function Packages:isInstalled(package)
	return self:installed()[package]
end

function Packages:downloadList()
	local packages = {
		[ 'develop-1.8' ] = 'https://pastebin.com/raw/udgPHf1m',
		[ 'master-1.8' ] = 'https://pastebin.com/raw/zG0Raihg',
	}

	if packages[_G.OPUS_BRANCH] then
		Util.download(packages[_G.OPUS_BRANCH], 'usr/config/packages')
	end
end

function Packages:getManifest(package)
	local fname = 'packages/' .. package .. '/.package'
	if fs.exists(fname) then
		local c = Util.readTable(fname)
		if c then
			c.repository = c.repository:gsub('{{OPUS_BRANCH}}', _G.OPUS_BRANCH)
			return c
		end
	end
	local list = self:list()
	local url = list and list[package]

	if url then
		local c = Util.httpGet(url)
		if c then
			c = textutils.unserialize(c)
			if c then
				c.repository = c.repository:gsub('{{OPUS_BRANCH}}', _G.OPUS_BRANCH)
				return c
			end
		end
	end
end

return Packages
