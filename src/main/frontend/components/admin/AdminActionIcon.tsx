import {
  faArrowsRotate,
  faBan,
  faFloppyDisk,
  faPen,
  faPlus,
  faRotate,
  faTrash,
  faUserMinus,
  faUserPlus,
} from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";

type AdminAction =
  "add" | "assign" | "delete" | "edit" | "regenerate" | "remove" | "revoke" | "rotate" | "save";

const icons = {
  add: faPlus,
  assign: faUserPlus,
  delete: faTrash,
  edit: faPen,
  regenerate: faArrowsRotate,
  remove: faUserMinus,
  revoke: faBan,
  rotate: faRotate,
  save: faFloppyDisk,
} as const;

export function AdminActionIcon({ action }: { action: AdminAction }) {
  return <FontAwesomeIcon aria-hidden className="me-2" fixedWidth icon={icons[action]} />;
}
