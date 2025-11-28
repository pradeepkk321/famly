package io.famly.mapper.core.model;

public class CodeMappingResult {
    private String code;
    private String system;
    private String display;
    
	public CodeMappingResult(String code, String system, String display) {
		super();
		this.code = code;
		this.system = system;
		this.display = display;
	}
	
    public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getSystem() {
		return system;
	}

	public void setSystem(String system) {
		this.system = system;
	}

	public String getDisplay() {
		return display;
	}

	public void setDisplay(String display) {
		this.display = display;
	}

	@Override
    public String toString() {
        return "CodeMappingResult{" +
            "code='" + code + '\'' +
            ", system='" + system + '\'' +
            ", display='" + display + '\'' +
            '}';
    }
	
	

}
