const fs = require('fs');
const path = require('path');

// Read files
const styles = fs.readFileSync(path.join(__dirname, 'admin-styles.css'), 'utf8');
const script = fs.readFileSync(path.join(__dirname, 'admin-script.js'), 'utf8');
const template = fs.readFileSync(path.join(__dirname, 'admin-template.html'), 'utf8');

// Replace placeholders
const html = template
	.replace('{{STYLES}}', styles)
	.replace('{{SCRIPT}}', script);

// Read current index.ts
const indexPath = path.join(__dirname, 'src', 'index.ts');
let indexContent = fs.readFileSync(indexPath, 'utf8');

// Find and replace getAdminHTML function
const functionStart = indexContent.indexOf('function getAdminHTML(): string {');
const functionEnd = indexContent.lastIndexOf('}') + 1;

if (functionStart === -1) {
	console.error('Could not find getAdminHTML function');
	process.exit(1);
}

// Create new function with escaped HTML
const escapedHtml = html
	.replace(/\\/g, '\\\\')
	.replace(/`/g, '\\`')
	.replace(/\$/g, '\\$');

const newFunction = `function getAdminHTML(): string {
	return \`${escapedHtml}\`;
}
`;

// Replace the function
const before = indexContent.substring(0, functionStart);
indexContent = before + newFunction;

// Write back
fs.writeFileSync(indexPath, indexContent, 'utf8');

console.log('✓ Admin panel HTML updated successfully!');
console.log('✓ Run "npm run deploy" to deploy changes');
