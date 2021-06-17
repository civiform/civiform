/** 
 * Client side formatting for text so that we can preview in the admin interface.
 * 
 * Pretty much just a TS port of TextFormatter.java
 */
class Formatter {
    static accordionContent = '>';
    static accordionHeader = '### ';
    static bulletedItem = '### ';


    static formatText(text: string, preserveEmptyLines: boolean): Element {
        const ret = document.createElement('div');
        const lines = text.split('\n');
        for (let i = 0; i < lines.length; i++) {
            let currentLine = lines[i].trim();
            if (currentLine.startsWith(this.accordionHeader)) {
                let title = currentLine.substring(3);
                let content = '';
                let next = i + 1;
                while (next < lines.length && lines[next].startsWith(this.accordionContent)) {
                    content += lines[next].substring(1) + '\n';
                    next++;
                }
                i = next - 1;
                ret.appendChild(Formatter.buildAccordion(title, content));
            } else if (currentLine.startsWith(this.bulletedItem)) {
                let listItems = [currentLine.substring(1).trim()];
                let next = i + 1;
                while (next < lines.length && lines[next].startsWith(this.bulletedItem)) {
                    listItems.push(lines[next].substring(1).trim());
                    next++;
                }
                i = next - 1;
                ret.appendChild(Formatter.buildList(listItems));
            } else if (currentLine.length > 0) {
                const content = document.createElement('div');
                content.textContent = currentLine;
                ret.appendChild(content);
            } else if (preserveEmptyLines) {
                const emptyLine = document.createElement('div');
                emptyLine.classList.add('h-6');
                ret.appendChild(emptyLine);
            }
        }
        return ret;
    }

    static buildAccordion(title: string, content: string): Element {
        let accordionContent = 
            Formatter.formatText(content, /* preserveEmptyLines = */ true);
        return AccordionController.build(title, accordionContent);
    }

    static buildList(items: string[]): Element {
        const listTag = document.createElement('ul');
        items.forEach(item => {
            const listItem = document.createElement('li');
            listItem.textContent = item;
            listTag.appendChild(listItem);
        });
        return listTag;
    }
}