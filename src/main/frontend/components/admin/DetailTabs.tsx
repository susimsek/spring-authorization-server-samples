"use client";

import Link from "next/link";
import { Nav } from "react-bootstrap";

export type DetailTab = { key: string; label: string; href?: string };

export function DetailTabs({ tabs, active }: { tabs: DetailTab[]; active: string }) {
  return (
    <div className="admin-detail-tabs-wrap">
      <Nav className="admin-detail-tabs flex-nowrap" activeKey={active}>
        {tabs.map((tab) => (
          <Nav.Item key={tab.key}>
            {tab.href ? (
              <Nav.Link as={Link} eventKey={tab.key} href={tab.href}>
                {tab.label}
              </Nav.Link>
            ) : (
              <Nav.Link eventKey={tab.key}>{tab.label}</Nav.Link>
            )}
          </Nav.Item>
        ))}
      </Nav>
    </div>
  );
}
