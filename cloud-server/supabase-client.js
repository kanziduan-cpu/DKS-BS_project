const { createClient } = require('@supabase/supabase-js');

// 从Supabase Dashboard获取这些值
// Settings → API
const supabaseUrl = process.env.SUPABASE_URL || 'YOUR_SUPABASE_URL';
const supabaseKey = process.env.SUPABASE_KEY || 'YOUR_SUPABASE_KEY';

// 创建Supabase客户端
const supabase = createClient(supabaseUrl, supabaseKey);

module.exports = supabase;
