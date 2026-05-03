import { Button, Card, Form, Space } from "antd";

export default function QueryBar({ children, onSearch, onReset, loading }) {
  return (
    <Card size="small" styles={{ body: { paddingBottom: 4 } }} style={{ marginBottom: 20 }}>
      <Form layout="inline" style={{ rowGap: 8 }}>
        {children}
        <Form.Item>
          <Space>
            <Button type="primary" onClick={onSearch} loading={loading}>
              查询
            </Button>
            <Button onClick={onReset}>重置</Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  );
}
