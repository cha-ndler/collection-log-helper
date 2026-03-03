package com.collectionloghelper.data;

import java.util.List;
import lombok.Value;

@Value
public class SourceRequirements
{
	List<String> quests;
	List<SkillRequirement> skills;
}
