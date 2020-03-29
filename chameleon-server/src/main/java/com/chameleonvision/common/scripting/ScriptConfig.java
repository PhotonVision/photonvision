package com.chameleonvision.common.scripting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ScriptConfig {
	public final ScriptEventType eventType;
	public final String command;

	public ScriptConfig(ScriptEventType eventType) {
		this.eventType = eventType;
		this.command = "";
	}

	@JsonCreator
	public ScriptConfig(
			@JsonProperty("eventType") ScriptEventType eventType,
			@JsonProperty("command") String command) {
		this.eventType = eventType;
		this.command = command;
	}
}
