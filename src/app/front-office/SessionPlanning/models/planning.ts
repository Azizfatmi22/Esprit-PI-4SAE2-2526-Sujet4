import { Schedule } from "./schedule";
import { Location } from "./location";
export interface Planning {
  id?: number;
  mode: PlanningMode;
  totalHours: number;
  startDate: Date;
  endDate: Date;
  sessionId?: number;
  location?: Location;
  schedules: Schedule[];
}
export enum PlanningMode {
  ONSITE = 'ONSITE',     // Formation en présentiel
  ONLINE = 'ONLINE',     // Formation à distance
  HYBRID = 'HYBRID'      // Mixte présentiel/distanciel
}
// Pour l'affichage des modes
export const PlanningModeLabels: Record<PlanningMode, string> = {
  [PlanningMode.ONSITE]: 'On-site',
  [PlanningMode.ONLINE]: 'Online',
  [PlanningMode.HYBRID]: 'Hybrid'
};

export const PlanningModeIcons: Record<PlanningMode, string> = {
  [PlanningMode.ONSITE]: '🏢',
  [PlanningMode.ONLINE]: '💻',
  [PlanningMode.HYBRID]: '🔄'
};

export const PlanningModeDescriptions: Record<PlanningMode, string> = {
  [PlanningMode.ONSITE]: 'In-person training at a physical location',
  [PlanningMode.ONLINE]: 'Fully online training via platform',
  [PlanningMode.HYBRID]: 'Mixed in-person and online training'
};
export const DaysOfWeek = [
  'Lundi',
  'Mardi',
  'Mercredi',
  'Jeudi',
  'Vendredi',
  'Samedi',
  'Dimanche'
];