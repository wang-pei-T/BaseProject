import { Card, Table } from "antd";

export default function TableCard({ title, extra, ...tableProps }) {
  return (
    <Card title={title} extra={extra} size="small" styles={{ header: { minHeight: 48 } }}>
      <Table size="middle" scroll={{ x: "max-content" }} {...tableProps} />
    </Card>
  );
}
