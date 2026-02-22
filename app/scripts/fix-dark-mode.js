#!/usr/bin/env node
/**
 * Script to automatically fix hardcoded colors for Dark Mode support
 * Replaces common patterns in all screen files
 */

const fs = require('fs');
const path = require('path');
const { glob } = require('glob');

const SCREENS_DIR = path.join(__dirname, '..', 'src', 'screens');

const REPLACEMENTS = [
  // Background colors
  { from: /backgroundColor:\s*['"]#fff['"]/g, to: "backgroundColor: theme.colors.surface" },
  { from: /backgroundColor:\s*['"]#ffffff['"]/gi, to: "backgroundColor: theme.colors.surface" },
  { from: /backgroundColor:\s*['"]#f5f5f5['"]/g, to: "backgroundColor: theme.colors.background" },
  { from: /backgroundColor:\s*['"]#fafafa['"]/g, to: "backgroundColor: theme.colors.background" },
  
  // Text colors
  { from: /color:\s*['"]#333['"]/g, to: "color: theme.colors.onSurface" },
  { from: /color:\s*['"]#666['"]/g, to: "color: theme.colors.onSurfaceVariant" },
  { from: /color:\s*['"]#999['"]/g, to: "color: theme.colors.onSurfaceVariant" },
  
  // Border colors
  { from: /borderColor:\s*['"]#e0e0e0['"]/g, to: "borderColor: theme.colors.surfaceVariant" },
  { from: /borderColor:\s*['"]#ddd['"]/g, to: "borderColor: theme.colors.outline" },
  { from: /borderTopColor:\s*['"]#e0e0e0['"]/g, to: "borderTopColor: theme.colors.surfaceVariant" },
  { from: /borderBottomColor:\s*['"]#e0e0e0['"]/g, to: "borderBottomColor: theme.colors.surfaceVariant" },
];

function fixFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf8');
  let modified = false;
  
  // Check if file already uses dynamic styles
  if (content.includes('createStyles') || content.includes('const styles = createStyles')) {
    console.log(`⏭️  Skipping ${path.basename(filePath)} (already uses dynamic styles)`);
    return false;
  }
  
  // Apply replacements
  REPLACEMENTS.forEach(({ from, to }) => {
    if (from.test(content)) {
      content = content.replace(from, to);
      modified = true;
    }
  });
  
  if (modified) {
    // Add useTheme import if not present
    if (!content.includes("from 'react-native-paper'") || !content.includes('useTheme')) {
      content = content.replace(
        /(import .* from 'react-native-paper';)/,
        "$1\nimport { useTheme } from 'react-native-paper';"
      );
    }
    
    // Convert static StyleSheet.create to function
    content = content.replace(
      /const styles = StyleSheet\.create\(/,
      'const createStyles = (theme: any) => StyleSheet.create('
    );
    
    // Add theme usage in component
    const componentMatch = content.match(/export const \w+Screen.*?{/);
    if (componentMatch) {
      const insertPoint = componentMatch.index + componentMatch[0].length;
      if (!content.includes('const theme = useTheme()')) {
        const indent = '  ';
        content = content.slice(0, insertPoint) + 
                 `\n${indent}const theme = useTheme();\n${indent}const styles = createStyles(theme);` +
                 content.slice(insertPoint);
      }
    }
    
    fs.writeFileSync(filePath, content, 'utf8');
    console.log(`✅ Fixed ${path.basename(filePath)}`);
    return true;
  }
  
  return false;
}

async function main() {
  console.log('🔍 Searching for screen files...\n');
  
  const files = await glob(`${SCREENS_DIR}/**/*Screen.tsx`);
  
  let fixedCount = 0;
  let skippedCount = 0;
  
  files.forEach(file => {
    if (fixFile(file)) {
      fixedCount++;
    } else {
      skippedCount++;
    }
  });
  
  console.log(`\n📊 Summary:`);
  console.log(`   ✅ Fixed: ${fixedCount} files`);
  console.log(`   ⏭️  Skipped: ${skippedCount} files`);
}

main().catch(console.error);
