export interface Schedule {
  id?: number;
  dayOfWeek: number; // 0 = Lundi, 6 = Dimanche
  startTime: string; // Format: "HH:mm"
  endTime: string;   // Format: "HH:mm"
  room?: string;
}