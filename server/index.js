import dotenv from 'dotenv'
import express from 'express'
import cors from 'cors'
import mysql from 'mysql2/promise'
import fs from 'fs'
import path from 'path'

dotenv.config()

const app = express()
app.use(cors())
app.use(express.json({ limit: '10mb' }))

const pool = mysql.createPool({
  host: process.env.MYSQL_HOST || '127.0.0.1',
  port: Number(process.env.MYSQL_PORT || 3306),
  user: process.env.MYSQL_USER || 'root',
  password: process.env.MYSQL_PASSWORD || '123456',
  database: process.env.MYSQL_DATABASE || 'chatrobot',
  connectionLimit: 4
})

const uploadDir = path.join(process.cwd(), 'server_uploads')
if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir, { recursive: true })
app.use('/static', express.static(uploadDir))

app.post('/messages', async (req, res) => {
  const { name, time, content, user_id, is_self, session_id, image_uri, status } = req.body || {}
  if (!name || !time || !content || !user_id || typeof is_self === 'undefined' || !session_id) return res.status(400).json({ ok: false })
  try {
    const sql = 'INSERT INTO messages (name,time,content,user_id,is_self,session_id,image_uri,status) VALUES (?,?,?,?,?,?,?,?)'
    const params = [name, time, content, user_id, is_self ? 1 : 0, session_id, image_uri || null, status || 'sent']
    const conn = await pool.getConnection()
    await conn.query(sql, params)
    conn.release()
    res.json({ ok: true })
  } catch (e) {
    res.status(500).json({ ok: false })
  }
})

app.get('/messages', async (req, res) => {
  const sessionId = Number(req.query.sessionId || 0)
  if (!sessionId) return res.status(400).json({ ok: false, data: [] })
  try {
    const sql = 'SELECT id,name,time,content,user_id,is_self,session_id,image_uri,status FROM messages WHERE session_id=? ORDER BY time ASC'
    const conn = await pool.getConnection()
    const [rows] = await conn.query(sql, [sessionId])
    conn.release()
    res.json({ ok: true, data: rows })
  } catch (e) {
    res.status(500).json({ ok: false, data: [] })
  }
})

app.post('/uploadImage', async (req, res) => {
  const b64 = req.body?.base64
  if (!b64) return res.status(400).json({ ok: false })
  try {
    const buf = Buffer.from(b64, 'base64')
    const name = `${Date.now()}_${Math.floor(Math.random()*1000)}.png`
    const file = path.join(uploadDir, name)
    await fs.promises.writeFile(file, buf)
    res.json({ ok: true, path: `/static/${name}` })
  } catch (e) {
    res.status(500).json({ ok: false })
  }
})

app.post('/users', async (req, res) => {
  const { account, password_hash, name } = req.body || {}
  if (!account || !password_hash) return res.status(400).json({ ok: false })
  try {
    const sql = 'INSERT INTO users (account,password_hash,name) VALUES (?,?,?) ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash), name=VALUES(name)'
    const params = [account, password_hash, name || '用户']
    const conn = await pool.getConnection()
    await conn.query(sql, params)
    conn.release()
    res.json({ ok: true })
  } catch (e) {
    res.status(500).json({ ok: false })
  }
})

app.post('/conversations', async (req, res) => {
  const { user_id, title } = req.body || {}
  if (!user_id) return res.status(400).json({ ok: false })
  try {
    const sql = 'INSERT INTO conversations (user_id,title) VALUES (?,?)'
    const conn = await pool.getConnection()
    const [ret] = await conn.query(sql, [user_id, title || '新对话'])
    conn.release()
    res.json({ ok: true, id: ret?.insertId || 0 })
  } catch (e) {
    res.status(500).json({ ok: false })
  }
})

const port = Number(process.env.PORT || 3000)
app.listen(port, () => {})
