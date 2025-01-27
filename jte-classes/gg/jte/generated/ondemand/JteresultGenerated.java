package gg.jte.generated.ondemand;
@SuppressWarnings("unchecked")
public final class JteresultGenerated {
	public static final String JTE_NAME = "result.jte";
	public static final int[] JTE_LINE_INFO = {0,0,0,0,0,8,8,8,8,25,34,34,34,0,0,0,0};
	public static void render(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, String content) {
		jteOutput.writeContent("\n<div class=\"bg-gray-50 rounded-lg border border-gray-200 p-4\">\n    <div class=\"relative\">\n        <textarea\n                id=\"result\"\n                class=\"w-full h-96 p-4 font-mono text-sm bg-white rounded-lg border border-gray-200 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none\"\n                readonly\n        >");
		jteOutput.setContext("textarea", null);
		jteOutput.writeUserContent(content);
		jteOutput.writeContent("</textarea>\n\n        <button\n                onclick=\"copyContent()\"\n                class=\"mt-4 inline-flex items-center px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg transition-colors duration-200\"\n        >\n            Copy Content\n        </button>\n    </div>\n</div>\n\n<script>\n    function copyContent() {\n        const textarea = document.getElementById('result');\n        textarea.select();\n        document.execCommand('copy');\n\n        ");
		jteOutput.writeContent("\n        const button = event.target;\n        const originalText = button.textContent;\n        button.textContent = 'Copied!';\n\n        setTimeout(() => {\n            button.textContent = originalText;\n        }, 2000);\n    }\n</script>");
	}
	public static void renderMap(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, java.util.Map<String, Object> params) {
		String content = (String)params.get("content");
		render(jteOutput, jteHtmlInterceptor, content);
	}
}
