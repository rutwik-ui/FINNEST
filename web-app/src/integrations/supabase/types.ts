export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  // Allows to automatically instantiate createClient with right options
  // instead of createClient<Database, { PostgrestVersion: 'XX' }>(URL, KEY)
  __InternalSupabase: {
    PostgrestVersion: "14.18"
  }
  public: {
    Tables: {
      finnest_activity: {
        Row: {
          detail: Json
          event: string
          id: number
          occurred_at: string
          platform: string
          user_id: string
        }
        Insert: {
          detail?: Json
          event: string
          id?: never
          occurred_at?: string
          platform?: string
          user_id?: string
        }
        Update: {
          detail?: Json
          event?: string
          id?: never
          occurred_at?: string
          platform?: string
          user_id?: string
        }
        Relationships: []
      }
      finnest_admin_audit: {
        Row: {
          action: string
          actor_id: string | null
          id: number
          item_id: string
          occurred_at: string
        }
        Insert: {
          action: string
          actor_id?: string | null
          id?: never
          item_id: string
          occurred_at?: string
        }
        Update: {
          action?: string
          actor_id?: string | null
          id?: never
          item_id?: string
          occurred_at?: string
        }
        Relationships: []
      }
      finnest_admin_roles: {
        Row: {
          created_at: string
          role: string
          user_id: string
        }
        Insert: {
          created_at?: string
          role: string
          user_id: string
        }
        Update: {
          created_at?: string
          role?: string
          user_id?: string
        }
        Relationships: []
      }
      finnest_app_settings: {
        Row: {
          payload: Json
          revision: number
          state: string
          updated_at: string
          updated_by: string | null
        }
        Insert: {
          payload: Json
          revision?: number
          state: string
          updated_at?: string
          updated_by?: string | null
        }
        Update: {
          payload?: Json
          revision?: number
          state?: string
          updated_at?: string
          updated_by?: string | null
        }
        Relationships: []
      }
      finnest_documents: {
        Row: {
          id: string
          journey_type: string
          name: string
          path: string
          uploaded_at: string
          user_id: string
        }
        Insert: {
          id?: string
          journey_type?: string
          name: string
          path: string
          uploaded_at?: string
          user_id?: string
        }
        Update: {
          id?: string
          journey_type?: string
          name?: string
          path?: string
          uploaded_at?: string
          user_id?: string
        }
        Relationships: []
      }
      finnest_draft_fields: {
        Row: {
          client_updated_at: string
          field_key: string
          journey_type: string
          updated_at: string
          user_id: string
          value: string
        }
        Insert: {
          client_updated_at: string
          field_key: string
          journey_type: string
          updated_at?: string
          user_id: string
          value?: string
        }
        Update: {
          client_updated_at?: string
          field_key?: string
          journey_type?: string
          updated_at?: string
          user_id?: string
          value?: string
        }
        Relationships: []
      }
      finnest_enquiries: {
        Row: {
          admin_note: string | null
          consent_contact: boolean
          contact_email: string | null
          contact_name: string | null
          contact_phone: string | null
          created_at: string
          id: string
          message: string
          project_id: string | null
          status: string
          subject: string
          topic: string
          updated_at: string
          user_id: string
        }
        Insert: {
          admin_note?: string | null
          consent_contact?: boolean
          contact_email?: string | null
          contact_name?: string | null
          contact_phone?: string | null
          created_at?: string
          id?: string
          message: string
          project_id?: string | null
          status?: string
          subject: string
          topic?: string
          updated_at?: string
          user_id: string
        }
        Update: {
          admin_note?: string | null
          consent_contact?: boolean
          contact_email?: string | null
          contact_name?: string | null
          contact_phone?: string | null
          created_at?: string
          id?: string
          message?: string
          project_id?: string | null
          status?: string
          subject?: string
          topic?: string
          updated_at?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "finnest_enquiries_project_id_fkey"
            columns: ["project_id"]
            isOneToOne: false
            referencedRelation: "finnest_projects"
            referencedColumns: ["id"]
          },
        ]
      }
      finnest_experts: {
        Row: {
          bio: string | null
          city: string | null
          created_at: string
          email: string | null
          id: string
          name: string
          phone: string | null
          specialty: string
          status: string
          updated_at: string
        }
        Insert: {
          bio?: string | null
          city?: string | null
          created_at?: string
          email?: string | null
          id?: string
          name: string
          phone?: string | null
          specialty: string
          status?: string
          updated_at?: string
        }
        Update: {
          bio?: string | null
          city?: string | null
          created_at?: string
          email?: string | null
          id?: string
          name?: string
          phone?: string | null
          specialty?: string
          status?: string
          updated_at?: string
        }
        Relationships: []
      }
      finnest_guidance: {
        Row: {
          body: string
          category: string
          created_at: string
          id: string
          status: string
          title: string
          updated_at: string
        }
        Insert: {
          body: string
          category: string
          created_at?: string
          id?: string
          status?: string
          title: string
          updated_at?: string
        }
        Update: {
          body?: string
          category?: string
          created_at?: string
          id?: string
          status?: string
          title?: string
          updated_at?: string
        }
        Relationships: []
      }
      finnest_journeys: {
        Row: {
          completed_steps: string[]
          created_at: string
          current_step: string | null
          journey_type: string
          payload: Json
          updated_at: string
          updated_by_device: string | null
          user_id: string
          version: number
        }
        Insert: {
          completed_steps?: string[]
          created_at?: string
          current_step?: string | null
          journey_type: string
          payload?: Json
          updated_at?: string
          updated_by_device?: string | null
          user_id: string
          version?: number
        }
        Update: {
          completed_steps?: string[]
          created_at?: string
          current_step?: string | null
          journey_type?: string
          payload?: Json
          updated_at?: string
          updated_by_device?: string | null
          user_id?: string
          version?: number
        }
        Relationships: []
      }
      finnest_profiles: {
        Row: {
          created_at: string
          email: string | null
          full_name: string | null
          last_seen_at: string | null
          updated_at: string
          user_id: string
        }
        Insert: {
          created_at?: string
          email?: string | null
          full_name?: string | null
          last_seen_at?: string | null
          updated_at?: string
          user_id: string
        }
        Update: {
          created_at?: string
          email?: string | null
          full_name?: string | null
          last_seen_at?: string | null
          updated_at?: string
          user_id?: string
        }
        Relationships: []
      }
      finnest_projects: {
        Row: {
          approval_note: string | null
          city: string | null
          configuration: string | null
          created_at: string
          developer: string | null
          id: string
          locality: string | null
          name: string
          possession_label: string | null
          price_from_label: string | null
          sort_order: number
          status: string
          summary: string | null
          updated_at: string
        }
        Insert: {
          approval_note?: string | null
          city?: string | null
          configuration?: string | null
          created_at?: string
          developer?: string | null
          id?: string
          locality?: string | null
          name: string
          possession_label?: string | null
          price_from_label?: string | null
          sort_order?: number
          status?: string
          summary?: string | null
          updated_at?: string
        }
        Update: {
          approval_note?: string | null
          city?: string | null
          configuration?: string | null
          created_at?: string
          developer?: string | null
          id?: string
          locality?: string | null
          name?: string
          possession_label?: string | null
          price_from_label?: string | null
          sort_order?: number
          status?: string
          summary?: string | null
          updated_at?: string
        }
        Relationships: []
      }
      finnest_settings_history: {
        Row: {
          actor_id: string | null
          id: number
          occurred_at: string
          payload: Json
          revision: number
          state: string
        }
        Insert: {
          actor_id?: string | null
          id?: never
          occurred_at?: string
          payload: Json
          revision: number
          state: string
        }
        Update: {
          actor_id?: string | null
          id?: never
          occurred_at?: string
          payload?: Json
          revision?: number
          state?: string
        }
        Relationships: []
      }
      finnest_support_tickets: {
        Row: {
          admin_note: string | null
          created_at: string
          id: string
          message: string
          status: string
          subject: string
          updated_at: string
          user_id: string
        }
        Insert: {
          admin_note?: string | null
          created_at?: string
          id?: string
          message: string
          status?: string
          subject: string
          updated_at?: string
          user_id?: string
        }
        Update: {
          admin_note?: string | null
          created_at?: string
          id?: string
          message?: string
          status?: string
          subject?: string
          updated_at?: string
          user_id?: string
        }
        Relationships: []
      }
      finnest_user_drafts: {
        Row: {
          created_at: string
          device_id: string
          journey_type: string
          payload: Json
          updated_at: string
          user_id: string
        }
        Insert: {
          created_at?: string
          device_id: string
          journey_type: string
          payload?: Json
          updated_at?: string
          user_id: string
        }
        Update: {
          created_at?: string
          device_id?: string
          journey_type?: string
          payload?: Json
          updated_at?: string
          user_id?: string
        }
        Relationships: []
      }
    }
    Views: {
      [_ in never]: never
    }
    Functions: {
      finnest_has_admin_role: { Args: { _roles: string[] }; Returns: boolean }
    }
    Enums: {
      [_ in never]: never
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, "public">]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends (DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never) = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends (PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never) = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  public: {
    Enums: {},
  },
} as const
