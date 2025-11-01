-- PhotonVision Time Synchronization Protocol Dissector
-- Protocol runs on UDP port 5810
-- Reference: https://docs.photonvision.org/en/v2026.0.0-alpha-1/docs/contributing/design-descriptions/time-sync.html

photon_timesync_proto = Proto("photon_timesync", "PhotonVision Time Sync Protocol")

-- Protocol fields
local pf_version = ProtoField.uint8("photon_timesync.version", "Version", base.DEC)
local pf_message_id = ProtoField.uint8("photon_timesync.message_id", "Message ID", base.DEC, {
  [0] = "Ping",
  [1] = "Pong"
})
local pf_client_time = ProtoField.uint64("photon_timesync.client_time", "Client Time (μs)", base.DEC)
local pf_server_time = ProtoField.uint64("photon_timesync.server_time", "Server Time (μs)", base.DEC)
local pf_response_in = ProtoField.framenum("photon_timesync.response_in", "Response In Frame", base.NONE,
  frametype.RESPONSE)
local pf_response_to = ProtoField.framenum("photon_timesync.response_to", "Response To Frame", base.NONE,
  frametype.REQUEST)
local pf_response_time = ProtoField.relative_time("photon_timesync.response_time", "Response Time")

-- Register fields
photon_timesync_proto.fields = {
  pf_version,
  pf_message_id,
  pf_client_time,
  pf_server_time,
  pf_response_in,
  pf_response_to,
  pf_response_time
}

-- Table to track ping/pong relationships
-- Key: client_time as string, Value: frame number of ping
local ping_table = {}
-- Table to store pong responses for pings
-- Key: ping frame number, Value: pong frame number
local pong_table = {}

-- Dissector function
function photon_timesync_proto.dissector(buffer, pinfo, tree)
  -- Check if buffer has minimum length (TspPing = 10 bytes)
  local length = buffer:len()
  if length < 10 then
    return 0
  end

  -- Set protocol column
  pinfo.cols.protocol = photon_timesync_proto.name

  -- Create protocol tree
  local subtree = tree:add(photon_timesync_proto, buffer(), "PhotonVision Time Sync Protocol Data")

  -- Parse version (1 byte)
  local version = buffer(0, 1):uint()
  subtree:add(pf_version, buffer(0, 1))

  -- Parse message_id (1 byte)
  local msg_id = buffer(1, 1):uint()
  subtree:add(pf_message_id, buffer(1, 1))

  -- Parse client_time (8 bytes, little-endian uint64)
  local client_time = buffer(2, 8):le_uint64()
  subtree:add_le(pf_client_time, buffer(2, 8))

  -- Convert client_time to string for use as key
  local client_time_key = tostring(client_time)
  local frame_num = pinfo.number

  -- Track relationships between ping and pong
  if not pinfo.visited then
    -- First pass: build the relationship tables
    if msg_id == 1 then
      -- This is a Ping - store it
      ping_table[client_time_key] = frame_num
    elseif msg_id == 2 then
      -- This is a Pong - find matching Ping
      local ping_frame = ping_table[client_time_key]
      if ping_frame then
        pong_table[ping_frame] = frame_num
      end
    end
  end

  -- Update info column and parse based on message type
  if msg_id == 1 then
    -- TspPing: version(1) + message_id(1) + client_time(8) = 10 bytes
    pinfo.cols.info = string.format("Time Sync Ping (client_time: %s μs)", tostring(client_time))

    -- Check if we have a response for this ping
    local pong_frame = pong_table[frame_num]
    if pong_frame then
      local response_item = subtree:add(pf_response_in, pong_frame)
      response_item:set_generated()
    end
  elseif msg_id == 2 then
    -- TspPong: TspPing + server_time(8) = 18 bytes
    pinfo.cols.info = "Time Sync Pong"

    if length >= 18 then
      local server_time = buffer(10, 8):le_uint64()
      subtree:add_le(pf_server_time, buffer(10, 8))
      pinfo.cols.info = string.format("Time Sync Pong (client: %s, server: %s μs)",
        tostring(client_time), tostring(server_time))

      -- Find the matching ping frame
      local ping_frame = ping_table[client_time_key]
      if ping_frame then
        local request_item = subtree:add(pf_response_to, ping_frame)
        request_item:set_generated()

        -- Calculate response time if we can get the ping packet
        local ping_time = pinfo.abs_ts - pinfo.rel_ts
        -- Note: This is an approximation. For accurate timing, we'd need to
        -- store the timestamp of the ping packet
      end
    end
  else
    pinfo.cols.info = string.format("Time Sync Unknown (ID: %d)", msg_id)
  end

  return length
end

-- Register dissector on UDP port 5810
local udp_port = DissectorTable.get("udp.port")
udp_port:add(5810, photon_timesync_proto)

-- Heuristic dissector function
local function heuristic_checker(buffer, pinfo, tree)
  local length = buffer:len()

  -- Check minimum length (TspPing = 10 bytes)
  if length < 10 then
    return false
  end

  local version = buffer(0, 1):uint()
  local msg_id = buffer(1, 1):uint()

  -- Check if this looks like our protocol
  -- Version should be reasonable (0-10), message_id should be 1 or 2
  if version <= 10 and (msg_id == 1 or msg_id == 2) then
    -- Validate packet structure
    if msg_id == 1 and length == 10 then
      -- TspPing is exactly 10 bytes
      photon_timesync_proto.dissector(buffer, pinfo, tree)
      return true
    elseif msg_id == 2 and length == 18 then
      -- TspPong is exactly 18 bytes
      photon_timesync_proto.dissector(buffer, pinfo, tree)
      return true
    end
  end

  return false
end

-- Register heuristic dissector
photon_timesync_proto:register_heuristic("udp", heuristic_checker)

-- Initialize function to reset tables on new capture
function photon_timesync_proto.init()
  ping_table = {}
  pong_table = {}
end
