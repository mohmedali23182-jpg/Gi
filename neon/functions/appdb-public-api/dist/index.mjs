import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const moduleValue = require('./bundle.cjs');
export default moduleValue.default ?? moduleValue;
