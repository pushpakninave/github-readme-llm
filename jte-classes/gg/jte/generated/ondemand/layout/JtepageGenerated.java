package gg.jte.generated.ondemand.layout;
import gg.jte.Content;
@SuppressWarnings("unchecked")
public final class JtepageGenerated {
	public static final String JTE_NAME = "layout/page.jte";
	public static final int[] JTE_LINE_INFO = {0,0,2,2,2,2,10,10,10,10,17,17,17,36,36,36,2,3,3,3,3};
	public static void render(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, String title, Content content) {
		jteOutput.writeContent("\n<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>");
		jteOutput.setContext("title", null);
		jteOutput.writeUserContent(title);
		jteOutput.writeContent("</title>\n    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/htmx/1.9.10/htmx.min.js\"></script>\n    <script src=\"https://cdn.tailwindcss.com\"></script>\n    <link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap\" rel=\"stylesheet\">\n</head>\n<body class=\"bg-gray-50 min-h-screen flex items-center justify-center p-4 font-sans\">\n<div class=\"w-full max-w-3xl mx-auto\">\n    ");
		jteOutput.setContext("div", null);
		jteOutput.writeUserContent(content);
		jteOutput.writeContent("\n</div>\n<script>\n    htmx.on('htmx:afterSettle', function(event) {\n        if (event.detail.target.id === 'result') {\n            event.detail.target.classList.add('fade-in');\n        }\n    });\n</script>\n<style>\n    @keyframes fadeIn {\n        from { opacity: 0; transform: translateY(10px); }\n        to { opacity: 1; transform: translateY(0); }\n    }\n    .fade-in {\n        animation: fadeIn 0.4s ease-out forwards;\n    }\n</style>\n</body>\n</html>");
	}
	public static void renderMap(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, java.util.Map<String, Object> params) {
		String title = (String)params.get("title");
		Content content = (Content)params.get("content");
		render(jteOutput, jteHtmlInterceptor, title, content);
	}
}
