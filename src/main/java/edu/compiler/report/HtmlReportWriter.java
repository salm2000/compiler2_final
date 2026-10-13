package edu.compiler.report;

import edu.compiler.ProjectResult;
import edu.compiler.ast.AstNode;
import edu.compiler.ast.AstPrinter;
import edu.compiler.symbols.Symbol;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

public final class HtmlReportWriter {
    public void writeAst(ProjectResult result, Path destination) throws IOException {
        AstPrinter printer = new AstPrinter();
        StringBuilder tree = new StringBuilder(result.python().accept(printer));
        result.templates().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(entry -> tree.append('\n').append(entry.getValue().accept(printer)));
        write(destination, page("AST report", "Python and template abstract syntax trees",
            graph(result, tree.toString())));
    }

    public void writeSymbols(ProjectResult result, Path destination) throws IOException {
        StringBuilder rows = new StringBuilder();
        result.symbols().symbols().stream()
            .sorted(Comparator.comparing(Symbol::scope).thenComparing(Symbol::name))
            .forEach(symbol -> rows.append("<tr><td>").append(escape(symbol.name()))
                .append("</td><td><span class=\"kind\">").append(escape(symbol.kind().toString()))
                .append("</span></td><td>").append(escape(symbol.scope()))
                .append("</td><td>").append(escape(symbol.location().toString()))
                .append("</td><td>").append(escape(symbol.inferredType())).append("</td></tr>"));
        String table = "<div class=\"table-wrap\"><table><thead><tr><th>Name</th><th>Kind</th><th>Scope</th><th>Location</th><th>Type</th></tr></thead><tbody>"
            + rows + "</tbody></table></div>";
        write(destination, page("Symbol table", "Collected Python and template symbols", table));
    }

    private void write(Path destination, String html) throws IOException {
        Path absolute = destination.toAbsolutePath();
        if (absolute.getParent() != null) Files.createDirectories(absolute.getParent());
        Files.writeString(absolute, html, StandardCharsets.UTF_8);
    }

    private String page(String title, String subtitle, String content) {
        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>%s</title>
              <style>
                :root { color-scheme: light dark; font-family: Inter, Segoe UI, sans-serif; }
                body { margin: 0; background: #0f172a; color: #e2e8f0; }
                header { padding: 2rem clamp(1rem, 5vw, 4rem); background: linear-gradient(135deg, #1d4ed8, #0f766e); }
                h1 { margin: 0 0 .4rem; font-size: 2rem; }
                header p { margin: 0; color: #dbeafe; }
                main { padding: 2rem clamp(1rem, 5vw, 4rem); }
                .tree { padding: 1.5rem; overflow: auto; line-height: 1.55; border: 1px solid #334155; border-radius: 12px; background: #111827; color: #d1fae5; }
                .graph-toolbar { display: flex; flex-wrap: wrap; gap: .6rem; align-items: center; margin-bottom: 1rem; }
                .graph-toolbar button { cursor: pointer; padding: .55rem .8rem; color: #e2e8f0; background: #1e293b; border: 1px solid #475569; border-radius: 7px; }
                .graph-toolbar button:hover { background: #334155; }
                .legend { margin-left: auto; color: #94a3b8; font-size: .9rem; }
                .dot { display: inline-block; width: .7rem; height: .7rem; margin: 0 .25rem 0 .7rem; border-radius: 50%%; }
                .dot.python { background: #2563eb; } .dot.template { background: #0f766e; }
                .graph-viewport { height: 72vh; min-height: 480px; overflow: auto; border: 1px solid #334155; border-radius: 12px; background: #080f1e; }
                .graph-viewport svg { display: block; }
                .edge { stroke: #475569; stroke-width: 1.5; fill: none; }
                .node { cursor: pointer; }
                .node rect { stroke-width: 1.5; }
                .node.python rect { fill: #172554; stroke: #3b82f6; }
                .node.template rect { fill: #134e4a; stroke: #2dd4bf; }
                .node.root rect { fill: #3b0764; stroke: #c084fc; }
                .node text { fill: #f8fafc; font: 12px ui-monospace, SFMono-Regular, Consolas, monospace; pointer-events: none; }
                .node .location { fill: #94a3b8; font-size: 10px; }
                .toggle { fill: #f8fafc; stroke: #0f172a; stroke-width: 1; }
                details { margin-top: 1rem; }
                summary { cursor: pointer; color: #93c5fd; padding: .8rem 0; }
                .table-wrap { overflow-x: auto; border: 1px solid #334155; border-radius: 12px; }
                table { width: 100%%; border-collapse: collapse; background: #111827; }
                th, td { padding: .8rem 1rem; text-align: left; border-bottom: 1px solid #334155; white-space: nowrap; }
                th { color: #93c5fd; background: #1e293b; }
                tr:hover td { background: #172033; }
                .kind { padding: .2rem .5rem; border-radius: 999px; background: #164e63; color: #cffafe; font-size: .8rem; }
              </style>
            </head>
            <body><header><h1>%s</h1><p>%s</p></header><main>%s</main></body>
            </html>
            """.formatted(escape(title), escape(title), escape(subtitle), content);
    }

    static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String graph(ProjectResult result, String textTree) {
        StringBuilder data = new StringBuilder("{\"label\":\"Compilation unit\",\"location\":\"\",\"category\":\"root\",\"children\":[");
        appendJsonNode(data, result.python());
        result.templates().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            data.append(','); appendJsonNode(data, entry.getValue());
        });
        data.append("]}");
        String content = """
            <section aria-label="Interactive AST graph">
              <div class="graph-toolbar">
                <button id="zoomIn" type="button">Zoom in</button>
                <button id="zoomOut" type="button">Zoom out</button>
                <button id="fitGraph" type="button">Fit width</button>
                <button id="expandAll" type="button">Expand all</button>
                <button id="collapseAll" type="button">Collapse branches</button>
                <span id="zoomLabel">100%</span>
                <span class="legend"><span class="dot python"></span>Python <span class="dot template"></span>Template</span>
              </div>
              <div id="graphViewport" class="graph-viewport"><svg id="astGraph" role="img" aria-label="AST node-link tree"><g id="edgeLayer"></g><g id="nodeLayer"></g></svg></div>
            </section>
            <details><summary>Searchable text tree</summary><pre class="tree">__TEXT_TREE__</pre></details>
            <script>
            (() => {
              const astData = __AST_DATA__;
              const svg = document.getElementById('astGraph');
              const viewport = document.getElementById('graphViewport');
              const edgesLayer = document.getElementById('edgeLayer');
              const nodesLayer = document.getElementById('nodeLayer');
              const zoomLabel = document.getElementById('zoomLabel');
              const NS = 'http://www.w3.org/2000/svg';
              let scale = 1, baseWidth = 800, baseHeight = 500;

              function element(name, attributes = {}) {
                const result = document.createElementNS(NS, name);
                for (const [key, value] of Object.entries(attributes)) result.setAttribute(key, value);
                return result;
              }
              function short(value, size) { return value.length > size ? value.slice(0, size - 1) + '…' : value; }
              function setCollapsed(node, collapsed) {
                if (node.children && node.children.length) {
                  node.collapsed = collapsed;
                  node.children.forEach(child => setCollapsed(child, collapsed));
                }
              }
              function applyScale() {
                svg.style.width = `${Math.round(baseWidth * scale)}px`;
                svg.style.height = `${Math.round(baseHeight * scale)}px`;
                zoomLabel.textContent = `${Math.round(scale * 100)}%`;
              }
              function render() {
                edgesLayer.replaceChildren(); nodesLayer.replaceChildren();
                const nodes = [], edges = []; let row = 0, deepest = 0;
                function layout(node, depth, parent) {
                  deepest = Math.max(deepest, depth);
                  const visible = node.collapsed ? [] : (node.children || []);
                  visible.forEach(child => layout(child, depth + 1, node));
                  node.x = 28 + depth * 270;
                  node.y = visible.length ? visible.reduce((sum, child) => sum + child.y, 0) / visible.length : 38 + row++ * 70;
                  nodes.push(node); if (parent) edges.push([parent, node]);
                }
                layout(astData, 0, null);
                baseWidth = Math.max(800, 28 + (deepest + 1) * 270);
                baseHeight = Math.max(500, 80 + Math.max(row, 1) * 70);
                svg.setAttribute('viewBox', `0 0 ${baseWidth} ${baseHeight}`);
                for (const [parent, child] of edges) {
                  const path = element('path', {class: 'edge', d: `M ${parent.x + 220} ${parent.y} C ${parent.x + 242} ${parent.y}, ${child.x - 22} ${child.y}, ${child.x} ${child.y}`});
                  edgesLayer.append(path);
                }
                for (const node of nodes) {
                  const group = element('g', {class: `node ${node.category}`, transform: `translate(${node.x} ${node.y - 23})`, tabindex: '0'});
                  group.append(element('rect', {width: 220, height: 46, rx: 8}));
                  const label = element('text', {x: 10, y: 18}); label.textContent = short(node.label, 31); group.append(label);
                  const location = element('text', {class: 'location', x: 10, y: 35}); location.textContent = short(node.location, 36); group.append(location);
                  const title = element('title'); title.textContent = `${node.label}${node.location ? ' @ ' + node.location : ''}`; group.append(title);
                  if (node.children && node.children.length) {
                    group.append(element('circle', {class: 'toggle', cx: 207, cy: 23, r: 7}));
                    const sign = element('text', {x: node.collapsed ? 203 : 204, y: 27}); sign.textContent = node.collapsed ? '+' : '−'; group.append(sign);
                    const toggle = () => { node.collapsed = !node.collapsed; render(); };
                    group.addEventListener('click', toggle);
                    group.addEventListener('keydown', event => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); toggle(); } });
                  }
                  nodesLayer.append(group);
                }
                applyScale();
              }
              function changeZoom(delta) { scale = Math.min(2.5, Math.max(.2, scale + delta)); applyScale(); }
              document.getElementById('zoomIn').addEventListener('click', () => changeZoom(.15));
              document.getElementById('zoomOut').addEventListener('click', () => changeZoom(-.15));
              document.getElementById('fitGraph').addEventListener('click', () => { scale = Math.min(1, (viewport.clientWidth - 20) / baseWidth); applyScale(); viewport.scrollTo(0, 0); });
              document.getElementById('expandAll').addEventListener('click', () => { setCollapsed(astData, false); render(); });
              document.getElementById('collapseAll').addEventListener('click', () => { (astData.children || []).forEach(child => setCollapsed(child, true)); astData.collapsed = false; render(); });
              viewport.addEventListener('wheel', event => { if (event.ctrlKey) { event.preventDefault(); changeZoom(event.deltaY < 0 ? .1 : -.1); } }, {passive: false});
              render();
            })();
            </script>
            """;
        return content.replace("__AST_DATA__", data.toString()).replace("__TEXT_TREE__", escape(textTree));
    }

    private void appendJsonNode(StringBuilder out, AstNode node) {
        String label = node.kind() + (node.detail().isBlank() ? "" : "(" + node.detail() + ")");
        String category = node.getClass().getName().contains(".template.") ? "template" : "python";
        out.append("{\"label\":\"").append(json(label)).append("\",\"location\":\"")
            .append(json(node.location().toString())).append("\",\"category\":\"").append(category).append("\",\"children\":[");
        boolean first = true;
        for (AstNode child : node.children()) {
            if (!first) out.append(',');
            appendJsonNode(out, child); first = false;
        }
        out.append("]}");
    }

    private String json(String value) {
        StringBuilder out = new StringBuilder();
        for (char character : value.toCharArray()) {
            switch (character) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '<' -> out.append("\\u003c");
                case '>' -> out.append("\\u003e");
                case '&' -> out.append("\\u0026");
                default -> { if (character < 32) out.append("\\u%04x".formatted((int) character)); else out.append(character); }
            }
        }
        return out.toString();
    }
}

