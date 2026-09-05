import { library } from "@fortawesome/fontawesome-svg-core";
import type { IconDefinition } from "@fortawesome/fontawesome-svg-core";
import {
  faAddressCard,
  faAnglesLeft,
  faAnglesRight,
  faArrowLeft,
  faArrowRight,
  faArrowsRotate,
  faBan,
  faBars,
  faCheck,
  faCircle,
  faCircleCheck,
  faCircleInfo,
  faCircleQuestion,
  faCompass,
  faClock,
  faClockRotateLeft,
  faCopy,
  faCube,
  faDesktop,
  faEllipsisVertical,
  faEnvelope,
  faEye,
  faEyeSlash,
  faFilter,
  faFloppyDisk,
  faGaugeHigh,
  faGlobe,
  faHeartPulse,
  faKey,
  faLaptop,
  faLayerGroup,
  faLock,
  faMagnifyingGlass,
  faMoon,
  faPen,
  faPlus,
  faRightFromBracket,
  faRightToBracket,
  faRotate,
  faShieldHalved,
  faSun,
  faTrash,
  faUnlock,
  faUser,
  faUserCheck,
  faUserMinus,
  faUserPlus,
  faUserSlash,
  faUserShield,
  faUsers,
  faUpload,
  faXmark,
  faChevronLeft,
  faChevronRight,
} from "@fortawesome/free-solid-svg-icons";

export type ActionIconName =
  | "add"
  | "assign"
  | "back"
  | "cancel"
  | "check"
  | "copy"
  | "delete"
  | "disable"
  | "edit"
  | "enable"
  | "filter"
  | "impersonate"
  | "login"
  | "logout"
  | "manage"
  | "next"
  | "regenerate"
  | "remove"
  | "retry"
  | "revoke"
  | "rotate"
  | "save"
  | "search"
  | "send"
  | "submit"
  | "unlock"
  | "upload"
  | "verify"
  | "view";

export const actionIcons: Record<ActionIconName, IconDefinition> = {
  add: faPlus,
  assign: faUserPlus,
  back: faArrowLeft,
  cancel: faXmark,
  check: faCheck,
  copy: faCopy,
  delete: faTrash,
  disable: faUserSlash,
  edit: faPen,
  enable: faUserCheck,
  filter: faFilter,
  impersonate: faUser,
  login: faRightToBracket,
  logout: faRightFromBracket,
  manage: faKey,
  next: faArrowRight,
  regenerate: faArrowsRotate,
  remove: faUserMinus,
  retry: faRotate,
  revoke: faBan,
  rotate: faRotate,
  save: faFloppyDisk,
  search: faMagnifyingGlass,
  send: faEnvelope,
  submit: faArrowRight,
  unlock: faUnlock,
  upload: faUpload,
  verify: faCircleCheck,
  view: faEye,
};

let loaded = false;

export const loadIcons = () => {
  if (loaded) return;

  library.add(
    faAnglesLeft,
    faAnglesRight,
    faAddressCard,
    faArrowLeft,
    faArrowRight,
    faArrowsRotate,
    faBan,
    faBars,
    faCheck,
    faCircle,
    faCircleCheck,
    faCircleInfo,
    faCircleQuestion,
    faCompass,
    faChevronLeft,
    faChevronRight,
    faClock,
    faClockRotateLeft,
    faCopy,
    faCube,
    faDesktop,
    faEllipsisVertical,
    faEnvelope,
    faEye,
    faEyeSlash,
    faFilter,
    faFloppyDisk,
    faGaugeHigh,
    faGlobe,
    faHeartPulse,
    faKey,
    faLaptop,
    faLayerGroup,
    faLock,
    faMagnifyingGlass,
    faMoon,
    faPen,
    faPlus,
    faRightFromBracket,
    faRightToBracket,
    faRotate,
    faShieldHalved,
    faSun,
    faTrash,
    faUnlock,
    faUser,
    faUserCheck,
    faUserMinus,
    faUserPlus,
    faUserSlash,
    faUserShield,
    faUsers,
    faUpload,
    faXmark,
  );
  loaded = true;
};
